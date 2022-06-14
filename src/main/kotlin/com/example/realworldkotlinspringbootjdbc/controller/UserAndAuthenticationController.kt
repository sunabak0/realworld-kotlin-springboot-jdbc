package com.example.realworldkotlinspringbootjdbc.controller

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.service.UserService
import com.example.realworldkotlinspringbootjdbc.util.MySession
import com.example.realworldkotlinspringbootjdbc.util.MySessionJwt
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "User and Authentication")
class UserAndAuthenticationController(
    val userService: UserService,
    val mySessionJwt: MySessionJwt,
) {
    @PostMapping("/users")
    fun register(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        // TODO: try/catch
        val user = ObjectMapper()
            .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
            .readValue<NullableUser>(rawRequestBody!!)
        return when (val result = userService.register(user.email, user.password, user.username)) {
            is Either.Right -> {
                val registeredUser = result.value
                val session = MySession(registeredUser.userId, registeredUser.email)
                when (val token = mySessionJwt.encode(session)) {
                    is Either.Right -> {
                        val currentUser = User(
                            registeredUser.email.value,
                            registeredUser.username.value,
                            registeredUser.bio.value,
                            registeredUser.image.value,
                            token.value,
                        )
                        ResponseEntity(
                            ObjectMapper()
                                .enable(SerializationFeature.WRAP_ROOT_VALUE)
                                .writeValueAsString(currentUser),
                            HttpStatus.valueOf(201)
                        )
                    }
                    is Either.Left -> {
                        // JWTにencodeする時に失敗
                        ResponseEntity("", HttpStatus.valueOf(500))
                    }
                }
            }
            is Either.Left -> when (val usecaseError = result.value) {
                is UserService.RegisterError.ValidationErrors -> {
                    ResponseEntity(
                        ObjectMapper()
                            .writeValueAsString(
                                mapOf(
                                    "errors" to mapOf(
                                        "body" to usecaseError.errors
                                    )
                                )
                            ),
                        HttpStatus.valueOf(422)
                    )
                }
                is UserService.RegisterError.FailedRegister -> {
                    ResponseEntity(
                        "", HttpStatus.valueOf(500)
                    )
                }
                else -> {
                    val responseBody = "予期せぬエラーが発生しました${usecaseError.javaClass.simpleName}"
                    ResponseEntity(responseBody, HttpStatus.valueOf(500))
                }
            }
        }
    }

    @PostMapping("/users/login")
    fun login(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        val user = User(
            "hoge@example.com",
            "hoge-username",
            "hoge-bio",
            "hoge-image",
            "hoge-token",
        )
        return ResponseEntity(
            ObjectMapper()
                .enable(SerializationFeature.WRAP_ROOT_VALUE)
                .writeValueAsString(user),
            HttpStatus.valueOf(200)
        )
    }

    @GetMapping("/user")
    fun showCurrentUser(): ResponseEntity<String> {
        val user = User(
            "hoge@example.com",
            "hoge-username",
            "hoge-bio",
            "hoge-image",
            "hoge-token",
        )
        return ResponseEntity(
            ObjectMapper()
                .enable(SerializationFeature.WRAP_ROOT_VALUE)
                .writeValueAsString(user),
            HttpStatus.valueOf(200)
        )
    }

    @PutMapping("/user")
    fun update(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        val user = User(
            "hoge@example.com",
            "hoge-username",
            "hoge-bio",
            "hoge-image",
            "hoge-token",
        )
        return ResponseEntity(
            ObjectMapper()
                .enable(SerializationFeature.WRAP_ROOT_VALUE)
                .writeValueAsString(user),
            HttpStatus.valueOf(200)
        )
    }

    @JsonRootName(value = "user")
    data class User(
        @JsonProperty("email") val email: String,
        @JsonProperty("username") val username: String,
        @JsonProperty("bio") val bio: String,
        @JsonProperty("image") val image: String,
        @JsonProperty("token") val token: String,
    )

    @JsonIgnoreProperties(ignoreUnknown = true) // デシリアライズ時、利用していないkeyがあった時、それを無視する
    @JsonRootName(value = "user")
    data class NullableUser(
        @JsonProperty("email") val email: String?,
        @JsonProperty("password") val password: String?,
        @JsonProperty("username") val username: String?,
        @JsonProperty("bio") val bio: String?,
        @JsonProperty("image") val image: String?,
    )
}
