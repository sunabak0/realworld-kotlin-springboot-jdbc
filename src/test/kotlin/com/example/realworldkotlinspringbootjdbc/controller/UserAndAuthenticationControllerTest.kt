package com.example.realworldkotlinspringbootjdbc.controller

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.service.UserService
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class UserAndAuthenticationControllerTest {
    object DummyValidationError : MyError.ValidationError { override val message: String get() = "DummyValidation" }
    // TODO Pass Test
    @Test
    fun `ユーザー登録時、バリデーションエラーの場合、それらを表示するレスポンスを返す`() {
        class DummyUserService() : UserService {
            override fun register(
                email: String?,
                password: String?,
                username: String?
            ): Either<UserService.RegisterError, RegisteredUser> {
                return Either.Left(UserService.RegisterError.ValidationErrors(listOf(DummyValidationError)))
            }
        }
        val requestBody = """
            {
                "user": {}
            }
        """.trimIndent()
        val actual = UserAndAuthenticationController(DummyUserService()).register(requestBody)
        val expected = ResponseEntity("""{"errors":{"body":["DummyValidation"]}}""", HttpStatus.valueOf(422))
        assertThat(actual).isEqualTo(expected)
    }
}
