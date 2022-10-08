package com.example.realworldkotlinspringbootjdbc.presentation.shared

import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

object AuthorizationError {
    // TODO: 使っていない変数があるので削除
    fun handle(@Suppress("UnusedPrivateMember") error: MyAuth.Unauthorized): ResponseEntity<String> = ResponseEntity("", HttpStatus.valueOf(401))
}
