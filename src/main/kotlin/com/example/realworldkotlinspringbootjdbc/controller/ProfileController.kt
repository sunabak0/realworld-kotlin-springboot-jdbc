package com.example.realworldkotlinspringbootjdbc.controller

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.service.ProfileService
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
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
    val profileService: ProfileService
) {
    @GetMapping("/profiles/{username}")
    fun showProfile(): ResponseEntity<String> {
        val result = profileService.showProfile("hoge-username")
        when (result) {
            is Either.Right -> {
                println(result.value)
            }
            is Either.Left -> {}
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

    @JsonRootName(value = "profile")
    data class Profile(
        @JsonProperty("username") val username: String,
        @JsonProperty("bio") val bio: String,
        @JsonProperty("image") val image: String,
        @JsonProperty("following") val following: Boolean,
    )
}
