package com.example.realworldkotlinspringbootjdbc.controller.response

import com.example.realworldkotlinspringbootjdbc.util.MyError
import com.fasterxml.jackson.databind.ObjectMapper

fun serializeMyErrorListForResponseBody(errors: List<MyError>): String =
    ObjectMapper()
        .writeValueAsString(
            mapOf(
                "errors" to mapOf(
                    "body" to errors
                )
            )
        )
fun serializeUnexpectedErrorForResponseBody(errorMessage: String): String =
    ObjectMapper()
        .writeValueAsString(
            mapOf(
                "errors" to mapOf(
                    "body" to listOf(errorMessage)
                )
            )
        )
