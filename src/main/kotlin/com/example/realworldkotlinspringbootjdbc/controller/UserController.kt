package com.example.realworldkotlinspringbootjdbc.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController {
    @PostMapping("/api/users")
    fun register(): ResponseEntity<String> {
        return ResponseEntity(ObjectMapper().writeValueAsString(
            mapOf(
                "user" to mapOf(
                    "email" to "hoge@example.com",
                    "username" to "hoge",
                    "bio" to "hoge-bio",
                    "image" to "hoge-image",
                    "token" to "hoge-token",
                ),
            )
        ), HttpStatus.valueOf(200))
    }

    @PostMapping("/api/users/login")
    fun login(): ResponseEntity<String> {
        return ResponseEntity(ObjectMapper().writeValueAsString(
            mapOf(
                "user" to mapOf(
                    "email" to "hoge@example.com",
                    "username" to "hoge",
                    "bio" to "hoge-bio",
                    "image" to "hoge-image",
                    "token" to "hoge-token",
                ),
            )
        ), HttpStatus.valueOf(200))
    }

    @GetMapping("/api/user")
    fun showCurrentUser(): ResponseEntity<String> {
        return ResponseEntity(ObjectMapper().writeValueAsString(
            mapOf(
                "user" to mapOf(
                    "email" to "hoge@example.com",
                    "username" to "hoge",
                    "bio" to "hoge-bio",
                    "image" to "hoge-image",
                    "token" to "hoge-token",
                ),
            )
        ), HttpStatus.valueOf(200))
    }

    @PutMapping("/api/user")
    fun update(): ResponseEntity<String> {
        return ResponseEntity(ObjectMapper().writeValueAsString(
            mapOf(
                "user" to mapOf(
                    "email" to "hoge@example.com",
                    "username" to "hoge",
                    "bio" to "hoge-bio",
                    "image" to "hoge-image",
                    "token" to "hoge-token",
                ),
            )
        ), HttpStatus.valueOf(200))
    }

}