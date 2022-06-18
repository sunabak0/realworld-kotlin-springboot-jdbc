package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.presentation.response.CurrentUser
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeMyErrorListForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
import com.example.realworldkotlinspringbootjdbc.usecase.UserService
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
        //
        // TODO: try/catch
        //
        val user = ObjectMapper()
            .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
            .readValue<NullableUser>(rawRequestBody!!)
        return when (val result = userService.register(user.email, user.password, user.username)) {
            //
            // ユーザー登録に成功
            //
            is Right -> {
                val registeredUser = result.value
                val session = MySession(registeredUser.userId, registeredUser.email)
                when (val token = mySessionJwt.encode(session)) {
                    //
                    // 全て成功
                    //
                    is Right -> ResponseEntity(
                        CurrentUser.from(registeredUser, token.value).serializeWithRootName(),
                        HttpStatus.valueOf(201)
                    )
                    //
                    // ユーザーの登録は上手くいったが、JWTのエンコードで失敗
                    //
                    is Left -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("予期せぬエラーが発生しました(cause: ${mySessionJwt::class.simpleName})"),
                        HttpStatus.valueOf(500)
                    )
                }
            }
            //
            // ユーザー登録に失敗
            //
            is Left -> when (val usecaseError = result.value) {
                //
                // 原因: バリデーションエラー
                //
                is UserService.RegisterError.ValidationErrors -> ResponseEntity(
                    serializeMyErrorListForResponseBody(usecaseError.errors),
                    HttpStatus.valueOf(422)
                )
                //
                // 原因: DB周りのエラー
                //
                is UserService.RegisterError.FailedRegister -> ResponseEntity(
                    "DBエラー",
                    HttpStatus.valueOf(500)
                )
                //
                // 原因: 予期せぬエラー
                //
                else -> ResponseEntity(
                    "予期せぬエラーが発生しました(cause: ${usecaseError::class.simpleName})",
                    HttpStatus.valueOf(500)
                )
            }
        }
    }

    @PostMapping("/users/login")
    fun login(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        // val user = ObjectMapper()
        //    .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
        //    .readValue<NullableUser>(rawRequestBody!!)
        // when (val result = userService.login(user.email, user.password)) {
        //    //
        //    // ログインに成功
        //    //
        //    is Right -> {
        //        val registeredUser = result.value
        //        val session = MySession(registeredUser.userId, registeredUser.email)
        //        when (val token = mySessionJwt.encode(session)) {
        //            //
        //            // 全て成功
        //            //
        //            is Right -> ResponseEntity(
        //                CurrentUser.from(registeredUser, token.value).serializeWithRootName(),
        //                HttpStatus.valueOf(201)
        //            )
        //            //
        //            // ユーザーの登録は上手くいったが、JWTのエンコードで失敗
        //            //
        //            is Left -> ResponseEntity(
        //                serializeUnexpectedErrorForResponseBody("予期せぬエラーが発生しました(cause: ${mySessionJwt::class.simpleName.toString()})"),
        //                HttpStatus.valueOf(500)
        //            )
        //        }
        //    }
        //    //
        //    // ログインに失敗
        //    //
        //    is Left -> {
        //
        //    }
        // }
        val currentUser = CurrentUser(
            "hoge@example.com",
            "hoge-username",
            "hoge-bio",
            "hoge-image",
            "hoge-token",
        )
        return ResponseEntity(
            ObjectMapper()
                .enable(SerializationFeature.WRAP_ROOT_VALUE)
                .writeValueAsString(currentUser),
            HttpStatus.valueOf(200)
        )
    }

    @GetMapping("/user")
    fun showCurrentUser(): ResponseEntity<String> {
        val user = CurrentUser(
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
        val user = CurrentUser(
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
