package com.example.c001apk.util

import android.content.Context
import com.example.c001apk.constant.Constants
import com.example.c001apk.util.Utils.getBase64
import com.example.c001apk.util.Utils.getMD5
import com.example.c001apk.util.Utils.randomAndroidVersionRelease
import com.example.c001apk.util.Utils.randomBrand
import com.example.c001apk.util.Utils.randomDeviceModel
import com.example.c001apk.util.Utils.randomManufacturer
import com.example.c001apk.util.Utils.randomSdkInt
import org.mindrot.jbcrypt.BCrypt
import java.util.Random


object TokenDeviceUtils {

    fun randHexString(@Suppress("SameParameterValue") n: Int): String {
        Random().setSeed(System.currentTimeMillis())
        return (0 until n).joinToString("") {
            Random().nextInt(256).toString(16)
        }.uppercase()
    }

    fun getDeviceCode(regenerate: Boolean): String {
        if (regenerate) {
            PrefManager.apply {
                MANUFACTURER = randomManufacturer()
                BRAND = randomBrand()
                MODEL = randomDeviceModel()
                BUILDNUMBER = randHexString(16)
                SDK_INT = randomSdkInt()
                ANDROID_VERSION = randomAndroidVersionRelease()
                USER_AGENT =
                    "Dalvik/2.1.0 (Linux; U; Android $ANDROID_VERSION; ${MODEL} ${BUILDNUMBER}) (#Build; ${BRAND}; ${MODEL}; ${BUILDNUMBER}; $ANDROID_VERSION) +CoolMarket/${VERSION_NAME}-${VERSION_CODE}-${Constants.MODE}"
            }
        }
        val szlmId = if (PrefManager.SZLMID == "") randHexString(16) else PrefManager.SZLMID
        val mac = Utils.randomMacAddress()
        val manuFactor = PrefManager.MANUFACTURER
        val brand = PrefManager.BRAND
        val model = PrefManager.MODEL
        val buildNumber = PrefManager.BUILDNUMBER
        return DeviceCode.encode("$szlmId; ; ; $mac; $manuFactor; $brand; $model; $buildNumber; null")
    }

    fun String.getTokenV2(): String {
        val timeStamp = (System.currentTimeMillis() / 1000f).toString()

        val base64TimeStamp = timeStamp.getBase64()
        val md5TimeStamp = timeStamp.getMD5()
        val md5DeviceCode = this.getMD5()

        val token = "${Constants.APP_LABEL}?$md5TimeStamp$$md5DeviceCode&${Constants.APP_ID}"
        val base64Token = token.getBase64()
        val md5Base64Token = base64Token.getMD5()
        val md5Token = token.getMD5()

        val bcryptSalt = "${"$2a$10$$base64TimeStamp/$md5Token".substring(0, 31)}u"
        val bcryptResult = BCrypt.hashpw(md5Base64Token, bcryptSalt)

        return "v2${bcryptResult.replaceRange(0, 3, "$2y").getBase64()}"
    }

    fun getLastingDeviceCode(): String {
        if (PrefManager.xAppDevice == "")
            PrefManager.xAppDevice = getDeviceCode(true)
        return PrefManager.xAppDevice
    }

    fun getLastingInstallTime(context: Context): String {
        val sp = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        return sp.getString("INSTALL_TIME", null).let {
            it ?: System.currentTimeMillis().toString().apply {
                sp.edit().putString("INSTALL_TIME", this).apply()
            }
        }
    }

}