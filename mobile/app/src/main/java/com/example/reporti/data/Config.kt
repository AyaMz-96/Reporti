package com.example.reporti.data

object Config {
    // Physical phone connected by USB: run `adb reverse tcp:8090 tcp:8090` once (again after replugging the phone),
    // then "localhost" on the phone points to the PC.
    //
    // Other options:
    //  - Emulator:           "http://10.0.2.2:8090/api/"
    //  - Phone on Wi-Fi:     "http://<PC LAN IP>:8090/api/" (e.g. 10.162.191.20). The IP must also be listed in
    //                        res/xml/network_security_config.xml, and Windows Firewall must allow port 8090.
    const val API_BASE_URL = "http://localhost:8090/api/"

    // TEMPORARY until real authentication exists: must be the id of an existing row in the users table.
    const val CITIZEN_ID = 1L
}