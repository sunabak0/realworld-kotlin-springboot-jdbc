package com.example.realworldkotlinspringbootjdbc.presentation.shared

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

object AuthorizationError {
    fun handle(): ResponseEntity<String> = ResponseEntity("", HttpStatus.valueOf(401))
}
