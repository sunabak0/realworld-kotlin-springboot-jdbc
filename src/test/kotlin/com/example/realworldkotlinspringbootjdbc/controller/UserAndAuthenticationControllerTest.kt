package com.example.realworldkotlinspringbootjdbc.controller

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.service.UserService
import com.example.realworldkotlinspringbootjdbc.util.MyError
import com.example.realworldkotlinspringbootjdbc.util.MySession
import com.example.realworldkotlinspringbootjdbc.util.MySessionJwt
import com.example.realworldkotlinspringbootjdbc.util.MySessionJwtImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class UserAndAuthenticationControllerTest {
    @Nested
    class Register {
        private val requestBody = """
                {
                    "user": {}
                }
        """.trimIndent()

        private val mySessionJwtEncodeReturnString = object : MySessionJwt {
            override fun encode(session: MySession) = "dummy-jwt-token".right()
        }
        @Test
        fun `ユーザー登録時、Serivceが「RegisteredUser」を返し、JWTエンコードが成功する場合、201レスポンスを返す`() {
            val dummyRegisteredUser = RegisteredUser.newWithoutValidation(
                1,
                "dummy@example.com",
                "dummy-name",
                "dummy-bio",
                "dummy-image",
            )
            val registerReturnRegisteredUser = object : UserService {
                override fun register(
                    email: String?,
                    password: String?,
                    username: String?,
                ): Either<UserService.RegisterError, RegisteredUser> = dummyRegisteredUser.right()
            }
            val actual = UserAndAuthenticationController(
                registerReturnRegisteredUser,
                mySessionJwtEncodeReturnString,
            ).register(requestBody)
            val expected = ResponseEntity(
                """{"user":{"email":"dummy@example.com","username":"dummy-name","bio":"dummy-bio","image":"dummy-image","token":"dummy-jwt-token"}}""",
                HttpStatus.valueOf(201)
            )
            assertThat(actual).isEqualTo(expected)
        }
        @Test
        fun `ユーザー登録時、Serivceが「バリデーションエラー」を返す場合、422エラーレスポンスを返す`() {
            val dummyValidationError = object : MyError.ValidationError {
                override val message: String get() = "DummyValidationError"
                override val key: String get() = "DummyKey"
            }
            val registerReturnValidationError = object : UserService {
                override fun register(
                    email: String?,
                    password: String?,
                    username: String?,
                ): Either<UserService.RegisterError, RegisteredUser> {
                    return Either.Left(UserService.RegisterError.ValidationErrors(listOf(dummyValidationError)))
                }
            }
            val actual = UserAndAuthenticationController(registerReturnValidationError, MySessionJwtImpl).register(requestBody)
            val expected = ResponseEntity("""{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError"}]}}""", HttpStatus.valueOf(422))
            assertThat(actual).isEqualTo(expected)
        }
        @Test
        fun `ユーザー登録時、Serivceが「登録失敗」を返す場合、500エラーレスポンスを返す`() {
            val dummyError = object : MyError {}
            val registerReturnFailedRegisterError = object : UserService {
                override fun register(
                    email: String?,
                    password: String?,
                    username: String?,
                ): Either<UserService.RegisterError, RegisteredUser> =
                    UserService.RegisterError.FailedRegister(dummyError).left()
            }
            val actual = UserAndAuthenticationController(registerReturnFailedRegisterError, MySessionJwtImpl).register(requestBody)
            val expected = ResponseEntity("DBエラー", HttpStatus.valueOf(500))
            assertThat(actual).isEqualTo(expected)
        }
    }
}