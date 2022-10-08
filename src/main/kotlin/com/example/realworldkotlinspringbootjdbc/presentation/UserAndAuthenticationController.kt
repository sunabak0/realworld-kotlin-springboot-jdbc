package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.presentation.request.NullableUser
import com.example.realworldkotlinspringbootjdbc.presentation.response.CurrentUser
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeMyErrorListForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.shared.AuthorizationError
import com.example.realworldkotlinspringbootjdbc.usecase.user_and_authentication.LoginUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.user_and_authentication.RegisterUserUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.user_and_authentication.UpdateUserUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.example.realworldkotlinspringbootjdbc.util.MySession
import com.example.realworldkotlinspringbootjdbc.util.MySessionJwt
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class UserAndAuthenticationController(
    val mySessionJwt: MySessionJwt,
    val myAuth: MyAuth,
    val registerUserUseCase: RegisterUserUseCase,
    val loginUseCase: LoginUseCase,
    val updateUserUseCase: UpdateUserUseCase,
) {
    /**
     * ユーザー登録
     *
     * 成功例
     * $ curl -X POST --header 'Content-Type: application/json' -d '{"user":{"email":"dummy@example.com", "password":"Passw0rd", "username":"taro"}}' 'http://localhost:8080/api/users' | jq '.'
     *
     * 失敗例
     * $ curl -X POST --header 'Content-Type: application/json' -d '{"user":{"email":"dummy@example.com"}}' 'http://localhost:8080/api/users' | jq '.'
     */
    @PostMapping("/users")
    fun register(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        val user = NullableUser.from(rawRequestBody)
        return when (val result = registerUserUseCase.execute(user.email, user.password, user.username)) {
            /**
             * ユーザー登録に成功
             */
            is Right -> {
                val registeredUser = result.value
                val session = MySession(registeredUser.userId, registeredUser.email)
                when (val token = mySessionJwt.encode(session)) {
                    /**
                     * 全て成功
                     */
                    is Right -> ResponseEntity(
                        CurrentUser.from(registeredUser, token.value).serializeWithRootName(),
                        HttpStatus.valueOf(201)
                    )
                    /**
                     * ユーザーの登録は上手くいったが、JWTのエンコードで失敗
                     */
                    is Left -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("予期せぬエラーが発生しました(cause: ${mySessionJwt::class.simpleName})"),
                        HttpStatus.valueOf(500)
                    )
                }
            }
            /**
             * ユーザー登録に失敗
             */
            is Left -> when (val useCaseError = result.value) {
                /**
                 * 原因: バリデーションエラー
                 */
                is RegisterUserUseCase.Error.InvalidUser -> ResponseEntity(
                    serializeMyErrorListForResponseBody(useCaseError.errors),
                    HttpStatus.valueOf(422)
                )
                /**
                 * 原因: 使おうとしたEmailが既に登録されている
                 */
                is RegisterUserUseCase.Error.AlreadyRegisteredEmail -> ResponseEntity(
                    serializeUnexpectedErrorForResponseBody("メールアドレスは既に登録されています"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                    HttpStatus.valueOf(422)
                )
                /**
                 * 原因: Usernameが既に登録されている
                 */
                is RegisterUserUseCase.Error.AlreadyRegisteredUsername -> ResponseEntity(
                    serializeUnexpectedErrorForResponseBody("ユーザー名は既に登録されています"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                    HttpStatus.valueOf(422)
                )
            }
        }
    }

    /**
     *
     * ログイン
     *
     * 例(成功/失敗)
     * $ curl -X POST --header 'Content-Type: application/json' -d '{"user":{"email":"1234@example.com", "password":"dummy-password"}}' 'http://localhost:8080/api/users/login' | jq '.'
     *
     * 失敗例
     * $ curl -X POST --header 'Content-Type: application/json' -d '{"user":{"email":"1234@example.com","password":""}}' 'http://localhost:8080/api/users/login' | jq '.'
     * $ curl -X POST --header 'Content-Type: application/json' -d '{"user":{"email":"1234@example.com"}}' 'http://localhost:8080/api/users/login' | jq '.'
     * $ curl -X POST --header 'Content-Type: application/json' -d '{"user":{"password":"dummy-password"}}' 'http://localhost:8080/api/users/login' | jq '.'
     * $ curl -X POST --header 'Content-Type: application/json' -d '{"user":{},,,,,}' 'http://localhost:8080/api/users/login' | jq '.'
     */
    @PostMapping("/users/login")
    fun login(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        val user = NullableUser.from(rawRequestBody)
        return when (val useCaseResult = loginUseCase.execute(user.email, user.password)) {
            /**
             * パスワード認証 成功
             */
            is Right -> {
                val registeredUser = useCaseResult.value
                val session = MySession(registeredUser.userId, registeredUser.email)
                when (val token = mySessionJwt.encode(session)) {
                    /**
                     * 全て成功
                     */
                    is Right -> ResponseEntity(
                        CurrentUser.from(registeredUser, token.value).serializeWithRootName(),
                        HttpStatus.valueOf(201)
                    )
                    /**
                     * 認証 は成功したが、JWTのエンコードで失敗
                     */
                    is Left -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("予期せぬエラーが発生しました(cause: ${mySessionJwt::class.simpleName})"),
                        HttpStatus.valueOf(500)
                    )
                }
            }
            /**
             * 何かしらに失敗
             */
            is Left -> when (val useCaseError = useCaseResult.value) {
                is LoginUseCase.Error.InvalidEmailOrPassword -> ResponseEntity(
                    serializeMyErrorListForResponseBody(useCaseError.errors),
                    HttpStatus.valueOf(401)
                )
                is LoginUseCase.Error.Unauthorized -> ResponseEntity(
                    serializeUnexpectedErrorForResponseBody("認証に失敗しました"),
                    HttpStatus.valueOf(401)
                )
            }
        }
    }

    /**
     *
     * (ログイン済みである)現在のユーザー情報を取得
     *
     * 例(成功/失敗)
     * $ curl -X GET --header 'Content-Type: application/json' -H 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJSZWFsV29ybGQiLCJ1c2VySWQiOjk5OSwiZW1haWwiOiJkdW1teUBleGFtcGxlLmNvbSJ9.zOw8VGLbw5vEFzWt__rbEW2mXg5InFyDYZ4bwuSZTtw' 'http://localhost:8080/api/user' | jq '.'
     *
     * 失敗例
     * $ curl -X GET --header 'Content-Type: application/json' -H 'Authorization: Bearer ***' 'http://localhost:8080/api/user' | jq '.'
     */
    @GetMapping("/user")
    fun showCurrentUser(@RequestHeader("Authorization") rawAuthorizationHeader: String?): ResponseEntity<String> =
        when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
            /**
             * JWT認証 失敗
             */
            is Left -> AuthorizationError.handle()
            /**
             * JWT認証 成功
             */
            is Right -> {
                val registeredUser = authorizeResult.value
                val session = MySession(registeredUser.userId, registeredUser.email)
                when (val token = mySessionJwt.encode(session)) {
                    /**
                     * 全て成功
                     */
                    is Right -> ResponseEntity(
                        CurrentUser.from(registeredUser, token.value).serializeWithRootName(),
                        HttpStatus.valueOf(201)
                    )
                    /**
                     * 原因: セッションのJWTエンコードで失敗
                     */
                    is Left -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("予期せぬエラーが発生しました(cause: ${mySessionJwt::class.simpleName})"),
                        HttpStatus.valueOf(500)
                    )
                }
            }
        }

    /**
     *
     * (ログイン済みである)現在のユーザー情報を更新する
     *
     * TODO: RealWorldの仕様にあるように見えないけど、Passwordを必須にした方が良さそう
     *
     * 成功例
     * $ TODO
     *
     * 失敗例
     * $ TODO
     */
    @PutMapping("/user")
    fun update(
        @RequestHeader("Authorization") rawAuthorizationHeader: String?,
        @RequestBody rawRequestBody: String?,
    ): ResponseEntity<String> {
        val nullableUser = NullableUser.from(rawRequestBody)
        return when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
            /**
             * JWT認証 失敗
             */
            is Left -> AuthorizationError.handle()
            /**
             * JWT認証 成功
             */
            is Right -> {
                val currentUser = authorizeResult.value
                val useCaseResult = updateUserUseCase.execute(
                    currentUser,
                    nullableUser.email,
                    nullableUser.username,
                    nullableUser.bio,
                    nullableUser.image,
                )
                when (useCaseResult) {
                    /**
                     * User情報更新 失敗
                     */
                    is Left -> when (val useCaseError = useCaseResult.value) {
                        /**
                         * 原因: 更新しようとした要素が異常
                         */
                        is UpdateUserUseCase.Error.InvalidAttributes -> ResponseEntity(
                            serializeMyErrorListForResponseBody(useCaseError.errors),
                            HttpStatus.valueOf(400)
                        )
                        /**
                         * 原因: emailが既に利用されている
                         */
                        is UpdateUserUseCase.Error.AlreadyUsedEmail -> ResponseEntity(
                            serializeUnexpectedErrorForResponseBody("メールアドレスは既に登録されています"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                            HttpStatus.valueOf(422)
                        )
                        /**
                         * 原因: usernameが既に利用されている
                         */
                        is UpdateUserUseCase.Error.AlreadyUsedUsername -> ResponseEntity(
                            serializeUnexpectedErrorForResponseBody("ユーザー名は既に登録されています"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                            HttpStatus.valueOf(422)
                        )
                        /**
                         * 原因: ユーザーが見つからなかった
                         */
                        is UpdateUserUseCase.Error.NotFoundUser -> ResponseEntity(
                            serializeUnexpectedErrorForResponseBody("ユーザーが見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                            HttpStatus.valueOf(404)
                        )
                    }
                    /**
                     * User情報更新 成功
                     */
                    is Right -> {
                        val session = MySession(currentUser.userId, currentUser.email)
                        when (val token = mySessionJwt.encode(session)) {
                            /**
                             * セッションのエンコード 成功
                             */
                            is Right -> ResponseEntity(
                                CurrentUser.from(useCaseResult.value, token.value).serializeWithRootName(),
                                HttpStatus.valueOf(200)
                            )
                            /**
                             * 原因: セッションのJWTエンコードで失敗
                             */
                            is Left -> ResponseEntity(
                                serializeUnexpectedErrorForResponseBody("予期せぬエラーが発生しました(cause: ${mySessionJwt::class.simpleName})"),
                                HttpStatus.valueOf(500)
                            )
                        }
                    }
                }
            }
        }
    }
}
