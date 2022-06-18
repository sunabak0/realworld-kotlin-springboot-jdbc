package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.presentation.response.Profile
import com.example.realworldkotlinspringbootjdbc.usecase.ShowProfileUseCase
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Profile")
class ProfileController(
    val showProfile: ShowProfileUseCase
) {
    @GetMapping("/profiles/{username}")
    fun showProfile(): ResponseEntity<String> {
        val result = showProfile.execute("hoge-username")
        when (result) {
            is Right -> {
                println(result.value)
            }
            is Left -> {}
        }
        val profile = Profile(
            "hoge-username",
            "hoge-bio",
            "hoge-image",
            true
        )
        return ResponseEntity(
            ObjectMapper()
                .enable(SerializationFeature.WRAP_ROOT_VALUE)
                .writeValueAsString(profile),
            HttpStatus.valueOf(200)
        )
    }

    @PostMapping("/profiles/{username}/follow")
    fun follow(): ResponseEntity<String> {
        val profile = Profile(
            "hoge-username",
            "hoge-bio", "hoge-image",
            true
        )
        return ResponseEntity(
            ObjectMapper()
                .enable(SerializationFeature.WRAP_ROOT_VALUE)
                .writeValueAsString(profile),
            HttpStatus.valueOf(200)
        )
    }

    @DeleteMapping("/profiles/{username}/follow")
    fun unfollow(): ResponseEntity<String> {
        val profile = Profile(
            "hoge-username",
            "hoge-bio",
            "hoge-image",
            false
        )
        return ResponseEntity(
            ObjectMapper()
                .enable(SerializationFeature.WRAP_ROOT_VALUE)
                .writeValueAsString(profile),
            HttpStatus.valueOf(200)
        )
    }
}
