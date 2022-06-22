package com.example.realworldkotlinspringbootjdbc.presentation.shared

import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

object AuthorizationError {
    fun handle(error: MyAuth.Unauthorized): ResponseEntity<String> =
        when (error) {
            /**
             * 原因: 謎
             */
            is MyAuth.Unauthorized.Unexpected -> ResponseEntity(
                serializeUnexpectedErrorForResponseBody("予期せぬエラーが発生しました(cause: $error)"),
                HttpStatus.valueOf(500)
            )
            /**
             * 原因: 謎以外全て
             */
            else -> ResponseEntity("", HttpStatus.valueOf(401))
        }
}
