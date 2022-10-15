package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.openapi.generated.controller.UserAndAuthenticationApi
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModel
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModelErrors
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.LoginUserRequest
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.NewUserRequest
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.User
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.UserResponse
import com.example.realworldkotlinspringbootjdbc.presentation.request.NullableUser
import com.example.realworldkotlinspringbootjdbc.presentation.response.CurrentUser
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeMyErrorListForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.shared.AuthorizationError
import com.example.realworldkotlinspringbootjdbc.presentation.shared.RealworldAuthenticationUseCaseUnauthorizedException
import com.example.realworldkotlinspringbootjdbc.presentation.shared.RealworldSessionEncodeErrorException
import com.example.realworldkotlinspringbootjdbc.usecase.shared.RealworldAuthenticationUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.user_and_authentication.LoginUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.user_and_authentication.RegisterUserUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.user_and_authentication.UpdateUserUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.example.realworldkotlinspringbootjdbc.util.MySession
import com.example.realworldkotlinspringbootjdbc.util.MySessionJwt
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class UserAndAuthenticationController(
    val mySessionJwt: MySessionJwt,
    val myAuth: MyAuth,
    val realworldAuthenticationUseCase: RealworldAuthenticationUseCase,
    val registerUserUseCase: RegisterUserUseCase,
    val loginUseCase: LoginUseCase,
    val updateUserUseCase: UpdateUserUseCase,
) : UserAndAuthenticationApi {

    override fun createUser(body: NewUserRequest): ResponseEntity<UserResponse> {
        val registeredUser = registerUserUseCase.execute(
            email = body.user.email,
            password = body.user.password,
            username = body.user.username,
        ).fold(
            { throw CreateUserUseCaseErrorException(it) },
            { it }
        )
        val token = mySessionJwt.encode(MySession(registeredUser.userId, registeredUser.email)).fold(
            { throw RealworldSessionEncodeErrorException(it) },
            { it }
        )
        return ResponseEntity(
            UserResponse(
                user = User(
                    email = registeredUser.email.value,
                    username = registeredUser.username.value,
                    bio = registeredUser.bio.value,
                    image = registeredUser.image.value,
                    token = token
                )
            ),
            HttpStatus.valueOf(201)
        )
    }

    data class CreateUserUseCaseErrorException(val error: RegisterUserUseCase.Error) : Exception()

    @ExceptionHandler(value = [CreateUserUseCaseErrorException::class])
    fun onCreateUserUseCaseErrorException(e: CreateUserUseCaseErrorException): ResponseEntity<GenericErrorModel> {
        val generateResponseEntity: (List<String>) -> ResponseEntity<GenericErrorModel> = { body ->
            ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = body)),
                HttpStatus.valueOf(422)
            )
        }
        return when (val error = e.error) {
            is RegisterUserUseCase.Error.AlreadyRegisteredEmail -> generateResponseEntity(listOf("メールアドレスは既に登録されています"))
            is RegisterUserUseCase.Error.AlreadyRegisteredUsername -> generateResponseEntity(listOf("ユーザー名は既に登録されています"))
            is RegisterUserUseCase.Error.InvalidUser -> generateResponseEntity(error.errors.map { it.message })
        }
    }

    override fun login(body: LoginUserRequest): ResponseEntity<UserResponse> {
        val registeredUser = loginUseCase.execute(
            email = body.user.email,
            password = body.user.password,
        ).fold(
            { throw LoginUseCaseErrorException(it) },
            { it }
        )
        val token: String = mySessionJwt.encode(MySession(registeredUser.userId, registeredUser.email)).fold(
            { throw RealworldSessionEncodeErrorException(it) },
            { it }
        )
        return ResponseEntity(
            UserResponse(
                user = User(
                    email = registeredUser.email.value,
                    username = registeredUser.username.value,
                    bio = registeredUser.bio.value,
                    image = registeredUser.image.value,
                    token = token
                )
            ),
            HttpStatus.valueOf(200)
        )
    }

    data class LoginUseCaseErrorException(val error: LoginUseCase.Error) : Exception()

    @ExceptionHandler(value = [LoginUseCaseErrorException::class])
    fun onLoginUseCaseErrorException(e: LoginUseCaseErrorException): ResponseEntity<GenericErrorModel> {
        val generateResponseEntity: (List<String>) -> ResponseEntity<GenericErrorModel> = { body ->
            ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = body)),
                HttpStatus.valueOf(401)
            )
        }
        return when (val error = e.error) {
            is LoginUseCase.Error.InvalidEmailOrPassword -> generateResponseEntity(error.errors.map { it.message })
            is LoginUseCase.Error.Unauthorized -> generateResponseEntity(listOf("認証に失敗しました"))
        }
    }

    /**
     * 現在ログイン中の登録済みユーザー取得
     * スキーマはOpenAPIで生成されたインターフェースを参照してください
     *
     * このactionで利用しているメインの UseCase は shared です（ログイン済みであることが前提である操作は全てこのUseCaseを利用します)
     * なので、例外もSharedなので、このコントローラで定義していません
     */
    override fun getCurrentUser(authorization: String): ResponseEntity<UserResponse> {
        val currentUser = realworldAuthenticationUseCase.execute(authorization).fold(
            { throw RealworldAuthenticationUseCaseUnauthorizedException(it) },
            { it }
        )
        val token = mySessionJwt.encode(MySession(currentUser.userId, currentUser.email)).fold(
            { throw RealworldSessionEncodeErrorException(it) },
            { it }
        )

        return ResponseEntity(
            UserResponse(
                user = User(
                    email = currentUser.email.value,
                    username = currentUser.username.value,
                    bio = currentUser.bio.value,
                    image = currentUser.image.value,
                    token = token
                )
            ),
            HttpStatus.valueOf(200)
        )
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
