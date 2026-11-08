package com.example.reporti.data

import android.os.Build

object Config {
    // Local Wi-Fi IP address of your PC: 192.168.100.27
    //  - Physical phone on Wi-Fi: "http://192.168.100.27:8090/api/"
    //  - Emulator:               "http://10.0.2.2:8090/api/"
    val API_BASE_URL: String = if (isEmulator) {
        "http://10.0.2.2:8090/api/"
    } else {
        "http://192.168.100.27:8090/api/"
    }

    // TEMPORARY until real authentication exists: must be the id of an existing row in the users table.
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