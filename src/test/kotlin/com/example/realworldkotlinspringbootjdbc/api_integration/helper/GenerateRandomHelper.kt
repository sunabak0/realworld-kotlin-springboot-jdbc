package com.example.realworldkotlinspringbootjdbc.api_integration.helper

object GenerateRandomHelper {
    fun getRandomString(length: Int): String {
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return List(length) { charset.random() }.joinToString("")
    }
}
