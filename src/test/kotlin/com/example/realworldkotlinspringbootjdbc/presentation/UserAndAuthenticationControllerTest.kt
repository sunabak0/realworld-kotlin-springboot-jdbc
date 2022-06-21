package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UnregisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Password
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.usecase.LoginUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.RegisterUserUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.example.realworldkotlinspringbootjdbc.util.MyError
import com.example.realworldkotlinspringbootjdbc.util.MySession
import com.example.realworldkotlinspringbootjdbc.util.MySessionJwt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class UserAndAuthenticationControllerTest {
    @Nested
    class `ユーザー登録(RegisterUserUseCase)` {
        private val requestBody = """
                {
                    "user": {}
                }
        """.trimIndent()

        private val notImplementedLoginUseCase = object : LoginUseCase {}
        private val notImplementedMyAuth = object : MyAuth {}
        private val mySessionJwtEncodeReturnString = object : MySessionJwt {
            override fun encode(session: MySession) = "dummy-jwt-token".right()
        }
        private fun userAndAuthenticationController(registerUserUseCase: RegisterUserUseCase): UserAndAuthenticationController =
            UserAndAuthenticationController(mySessionJwtEncodeReturnString, notImplementedMyAuth, registerUserUseCase, notImplementedLoginUseCase, )
        @Test
        fun `UseCase が「RegisteredUser」を返し、JWTエンコードが成功する場合、201レスポンスを返す`() {
            val dummyRegisteredUser = RegisteredUser.newWithoutValidation(
                1,
                "dummy@example.com",
                "dummy-name",
                "dummy-bio",
                "dummy-image",
            )
            val registerReturnRegisteredUser = object : RegisterUserUseCase {
                override fun execute(
                    email: String?,
                    password: String?,
                    username: String?,
                ): Either<RegisterUserUseCase.Error, RegisteredUser> = dummyRegisteredUser.right()
            }
            val actual = userAndAuthenticationController(registerReturnRegisteredUser).register(requestBody)
            val expected = ResponseEntity(
                """{"user":{"email":"dummy@example.com","username":"dummy-name","bio":"dummy-bio","image":"dummy-image","token":"dummy-jwt-token"}}""",
                HttpStatus.valueOf(201)
            )
            assertThat(actual).isEqualTo(expected)
        }
        @Test
        fun `UseCase が「バリデーションエラー」を返す場合、422エラーレスポンスを返す`() {
            val dummyValidationError = object : MyError.ValidationError {
                override val message: String get() = "DummyValidationError"
                override val key: String get() = "DummyKey"
            }
            val registerReturnValidationError = object : RegisterUserUseCase {
                override fun execute(
                    email: String?,
                    password: String?,
                    username: String?,
                ): Either<RegisterUserUseCase.Error, RegisteredUser> {
                    return RegisterUserUseCase.Error.InvalidUser(listOf(dummyValidationError)).left()
                }
            }
            val actual = userAndAuthenticationController(registerReturnValidationError).register(requestBody)
            val expected = ResponseEntity("""{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError"}]}}""", HttpStatus.valueOf(422))
            assertThat(actual).isEqualTo(expected)
        }
        @Test
        fun `UseCase が「登録失敗」を返す場合、500エラーレスポンスを返す`() {
            val dummyError = object : MyError {}
            val dummyRegisteredUser = object : UnregisteredUser {
                override val email: Email get() = object : Email { override val value: String get() = "dummy@example.com" }
                override val password: Password get() = object : Password { override val value: String get() = "dummy-password" }
                override val username: Username get() = object : Username { override val value: String get() = "dummy-username" }
            }
            val registerReturnFailedError = object : RegisterUserUseCase {
                override fun execute(
                    email: String?,
                    password: String?,
                    username: String?,
                ): Either<RegisterUserUseCase.Error, RegisteredUser> =
                    RegisterUserUseCase.Error.AlreadyRegisteredEmail(dummyError, dummyRegisteredUser).left()
            }
            val actual = userAndAuthenticationController(registerReturnFailedError).register(requestBody)
            val expected = ResponseEntity("""{"errors":{"body":["メールアドレスは既に登録されています"]}}""", HttpStatus.valueOf(422))
            assertThat(actual).isEqualTo(expected)
        }
    }
}
