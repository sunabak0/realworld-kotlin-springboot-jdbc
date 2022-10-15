package com.example.realworldkotlinspringbootjdbc.presentation

import com.example.realworldkotlinspringbootjdbc.openapi.generated.controller.UserAndAuthenticationApi
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModel
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModelErrors
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.LoginUserRequest
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.NewUserRequest
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.UpdateUserRequest
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.User
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.UserResponse
import com.example.realworldkotlinspringbootjdbc.presentation.shared.RealworldAuthenticationUseCaseUnauthorizedException
import com.example.realworldkotlinspringbootjdbc.presentation.shared.RealworldSessionEncodeErrorException
import com.example.realworldkotlinspringbootjdbc.usecase.shared.RealworldAuthenticationUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.user_and_authentication.LoginUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.user_and_authentication.RegisterUserUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.user_and_authentication.UpdateUserUseCase
import com.example.realworldkotlinspringbootjdbc.util.MySession
import com.example.realworldkotlinspringbootjdbc.util.MySessionJwt
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController

@RestController
class UserAndAuthenticationController(
    val mySessionJwt: MySessionJwt,
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

    override fun updateCurrentUser(authorization: String, body: UpdateUserRequest): ResponseEntity<UserResponse> {
        val currentUser = realworldAuthenticationUseCase.execute(authorization).fold(
            { throw RealworldAuthenticationUseCaseUnauthorizedException(it) },
            { it }
        )
        val updatedUser = updateUserUseCase.execute(
            currentUser = currentUser,
            email = body.user.email,
            username = body.user.username,
            bio = body.user.bio,
            image = body.user.image,
        ).fold(
            { throw UpdateUserUseCaseErrorException(it) },
            { it }
        )
        val newToken = mySessionJwt.encode(MySession(updatedUser.userId, updatedUser.email)).fold(
            { throw RealworldSessionEncodeErrorException(it) },
            { it }
        )

        return ResponseEntity(
            UserResponse(
                user = User(
                    email = updatedUser.email.value,
                    username = updatedUser.username.value,
                    bio = updatedUser.bio.value,
                    image = updatedUser.image.value,
                    token = newToken
                )
            ),
            HttpStatus.valueOf(200)
        )
    }

    data class UpdateUserUseCaseErrorException(val error: UpdateUserUseCase.Error) : Exception()

    @ExceptionHandler(value = [UpdateUserUseCaseErrorException::class])
    fun onUpdateUserUseCaseErrorException(e: UpdateUserUseCaseErrorException): ResponseEntity<GenericErrorModel> =
        when (val error = e.error) {
            is UpdateUserUseCase.Error.AlreadyUsedEmail -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("メールアドレスは既に登録されています"))),
                HttpStatus.valueOf(422)
            )
            is UpdateUserUseCase.Error.AlreadyUsedUsername -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("ユーザー名は既に登録されています"))),
                HttpStatus.valueOf(422)
            )
            is UpdateUserUseCase.Error.InvalidAttributes -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = error.errors.map { it.message })),
                HttpStatus.valueOf(400)
            )
            is UpdateUserUseCase.Error.NotFoundUser -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("セッション情報取得時にはユーザーは見つかりましたが、更新時にユーザーが見つかりませんでした(ほぼ有りえません)"))),
                HttpStatus.valueOf(422)
            )
        }
}
