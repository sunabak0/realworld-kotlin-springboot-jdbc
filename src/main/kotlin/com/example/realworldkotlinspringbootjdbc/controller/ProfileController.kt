package com.example.realworldkotlinspringbootjdbc.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Profile")
class ProfileController {
    @GetMapping("/profiles/{username}")
    fun showProfile(): ResponseEntity<String> {
        return ResponseEntity(
            ObjectMapper().writeValueAsString(
                mapOf(
                    "profile" to mapOf(
                        "username" to "hoge",
                        "bio" to "hoge-bio",
                        "image" to "hoge-image",
                        "following" to true,
                    ),
                )
            ),
            HttpStatus.valueOf(200)
        )
    }

    @PostMapping("/profiles/{username}/follow")
    fun follow(): ResponseEntity<String> {
        return ResponseEntity(
            ObjectMapper().writeValueAsString(
                mapOf(
                    "profile" to mapOf(
                        "username" to "hoge",
                        "bio" to "hoge-bio",
                        "image" to "hoge-image",
                        "following" to true,
                    ),
                )
            ),
            HttpStatus.valueOf(200)
        )
    }

    @DeleteMapping("/profiles/{username}/follow")
    fun unfollow(): ResponseEntity<String> {
        return ResponseEntity(
            ObjectMapper().writeValueAsString(
                mapOf(
                    "profile" to mapOf(
                        "username" to "hoge",
                        "bio" to "hoge-bio",
                        "image" to "hoge-image",
                        "following" to false,
                    ),
                )
            ),
            HttpStatus.valueOf(200)
        )
    }
}
