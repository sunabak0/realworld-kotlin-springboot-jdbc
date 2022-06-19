package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.presentation.response.CurrentUser
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeMyErrorListForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
import com.example.realworldkotlinspringbootjdbc.request.NullableUser
import com.example.realworldkotlinspringbootjdbc.usecase.RegisterUserUseCase
import com.example.realworldkotlinspringbootjdbc.util.MySession
import com.example.realworldkotlinspringbootjdbc.util.MySessionJwt
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
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
    val registerUser: RegisterUserUseCase,
    val mySessionJwt: MySessionJwt,
) {
    @PostMapping("/users")
    fun register(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        val user = NullableUser.from(rawRequestBody)
        return when (val result = registerUser.execute(user.email, user.password, user.username)) {
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
                is RegisterUserUseCase.Error.ValidationErrors -> ResponseEntity(
                    serializeMyErrorListForResponseBody(usecaseError.errors),
                    HttpStatus.valueOf(422)
                )
                //
                // 原因: DB周りのエラー
                //
                is RegisterUserUseCase.Error.FailedRegister -> ResponseEntity(
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
}
