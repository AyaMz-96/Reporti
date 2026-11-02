package com.example.reporti.data

object Config {
    // 10.0.2.2 is the emulator's alias for the PC's localhost.
    // A physical phone would need the PC's local network IP here (and the same IP in network_security_config.xml).
    const val API_BASE_URL = "http://10.0.2.2:8090/api/"

    // TEMPORARY until real authentication exists: must be the id of an existing row in the users table.
    const val CITIZEN_ID = 1L
}