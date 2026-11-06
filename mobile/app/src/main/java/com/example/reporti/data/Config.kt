package com.example.reporti.data

object Config {
    // Local Wi-Fi IP address of your PC running the Spring Boot backend
    const val API_BASE_URL = "http://192.168.1.123:8090/api/"

    // TEMPORARY until real authentication exists: must be the id of an existing row in the users table.
    const val CITIZEN_ID = 1L
}
