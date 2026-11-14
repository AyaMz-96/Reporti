package com.example.reporti.data

import android.os.Build

object Config {
    // Emulator: 10.0.2.2 is the alias for the host machine
    // Physical phone over USB: use 127.0.0.1 and run `adb reverse tcp:8090 tcp:8090`
    // Physical phone on WiFi: switch the else-branch back to "http://<PC-LAN-IP>:8090/api/"
    val API_BASE_URL: String = if (isEmulator) {
        "http://10.0.2.2:8090/api/"
    } else {
        "http://127.0.0.1:8090/api/"
    }

    const val CITIZEN_ID = 1L

    private val isEmulator: Boolean
        get() = (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
}