package com.example.realworldkotlinspringbootjdbc.controller

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.service.UserService
import com.example.realworldkotlinspringbootjdbc.util.MyError
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
        @Test
        fun `ユーザー登録時、Serivceが「RegisteredUser」を返す場合、201レスポンスを返す`() {
            val registeredUser = object : RegisteredUser {
                override val userId: UserId get() = UserId(1)
                override val email: Email get() = object : Email { override val value: String get() = "dummy@example.com" }
                override val username: Username get() = object : Username { override val value: String get() = "dummy-name" }
                override val bio: Bio get() = object : Bio { override val value: String get() = "dummy-bio" }
                override val image: Image get() = object : Image { override val value: String get() = "dummy-image" }
            }
            val registerReturnRegisteredUser = object : UserService {
                override fun register(
                    email: String?,
                    password: String?,
                    username: String?
                ): Either<UserService.RegisterError, RegisteredUser> {
                    return Either.Right(registeredUser)
                }
            }
            // TODO: Use MySessionJwt mock
            val actual = UserAndAuthenticationController(registerReturnRegisteredUser, MySessionJwtImpl).register(requestBody)
            val expected = ResponseEntity("""{"user":{"email":"dummy@example.com","username":"dummy-name","bio":"dummy-bio","image":"dummy-image","token":"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJSZWFsV29ybGQiLCJ1c2VySWQiOjEsImVtYWlsIjoiZHVtbXlAZXhhbXBsZS5jb20ifQ.3O4HqKP1eC70_PaXjRE8IIQsLT1WZU_WaSwtY5EfcmM"}}""", HttpStatus.valueOf(201))
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
                    username: String?
                ): Either<UserService.RegisterError, RegisteredUser> {
                    return Either.Left(UserService.RegisterError.ValidationErrors(listOf(dummyValidationError)))
                }
            }
            val actual = UserAndAuthenticationController(registerReturnValidationError, MySessionJwtImpl).register(requestBody)
            val expected = ResponseEntity("""{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError"}]}}""", HttpStatus.valueOf(422))
            assertThat(actual).isEqualTo(expected)
        }
        object DummyError : MyError
        @Test
        fun `ユーザー登録時、Serivceが「登録失敗」を返す場合、500エラーレスポンスを返す`() {
            val registerReturnFailedRegisterError = object : UserService {
                override fun register(
                    email: String?,
                    password: String?,
                    username: String?
                ): Either<UserService.RegisterError, RegisteredUser> {
                    return Either.Left(UserService.RegisterError.FailedRegister(DummyError))
                }
            }
            val actual = UserAndAuthenticationController(registerReturnFailedRegisterError, MySessionJwtImpl).register(requestBody)
            val expected = ResponseEntity("", HttpStatus.valueOf(500))
            assertThat(actual).isEqualTo(expected)
        }
    }
}
