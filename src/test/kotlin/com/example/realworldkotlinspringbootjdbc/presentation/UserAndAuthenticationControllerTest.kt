package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.Option
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import com.auth0.jwt.exceptions.JWTCreationException
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UnregisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.Password
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.usecase.user_and_authentication.LoginUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.user_and_authentication.RegisterUserUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.user_and_authentication.UpdateUserUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.example.realworldkotlinspringbootjdbc.util.MyError
import com.example.realworldkotlinspringbootjdbc.util.MySession
import com.example.realworldkotlinspringbootjdbc.util.MySessionJwt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.stream.Stream

class UserAndAuthenticationControllerTest {
    class `ユーザー登録2` {
        @ParameterizedTest
        @ArgumentsSource(TestCaseTable::class)
        fun test(
            useCaseResult: Either<RegisterUserUseCase.Error, RegisteredUser>,
            mySessionEncodeResult: Either.Right<String>?,
            expected: ResponseEntity<String>,
        ) {
            val mySessionJwt = Option.fromNullable(mySessionEncodeResult).fold(
                { object : MySessionJwt {} }, // mySessionEncodeResultがnullの時は関係ない
                { object : MySessionJwt { override fun encode(session: MySession): Either<MySessionJwt.EncodeError, String> = it } }
            )
            val actual = UserAndAuthenticationController(
                mySessionJwt,
                object : MyAuth {}, // 関係ない
                object :
                    RegisterUserUseCase { override fun execute(email: String?, password: String?, username: String?): Either<RegisterUserUseCase.Error, RegisteredUser> = useCaseResult },
                object : LoginUseCase {}, // 関係ない
                object : UpdateUserUseCase {}, // 関係ない
            ).register("""{"user": {}}""")

            assertThat(actual).isEqualTo(expected)
        }
        private class TestCaseTable : ArgumentsProvider {
            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(
                    Arguments.arguments(successByUseCase.right(), successByMySessionEncode.right(), ResponseEntity<String>("""{"user":{"email":"dummy@example.com","username":"dummy-name","bio":"dummy-bio","image":"dummy-image","token":"success.encoded.token"}}""", HttpStatus.valueOf(201))),
                )
            }
            val successByUseCase = RegisteredUser.newWithoutValidation(
                UserId(1),
                Email.newWithoutValidation("dummy@example.com"),
                Username.newWithoutValidation("dummy-name"),
                Bio.newWithoutValidation("dummy-bio"),
                Image.newWithoutValidation("dummy-image"),
            )
            val successByMySessionEncode = "success.encoded.token"
        }
    }

    class `ユーザー登録` {
        val dummyRegisteredUser = RegisteredUser.newWithoutValidation(
            UserId(1),
            Email.newWithoutValidation("dummy@example.com"),
            Username.newWithoutValidation("dummy-name"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
        )
        val dummyMySession = MySession(
            UserId(1),
            object : Email { override val value: String get() = "dummy@example.com" }
        )

        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<RegisterUserUseCase.Error, RegisteredUser>,
            val mySessionEncodeResult: Either<MySessionJwt.EncodeError, String>,
            val expected: ResponseEntity<String>,
        )

        @TestFactory
        fun registerUserTest(): Stream<DynamicNode> {
            val useCaseSuccess = dummyRegisteredUser
            val mySessionEncodeSuccess = "success.encoded.token"
            val mySessionEncodeFailed = MySessionJwt.EncodeError.FailedEncode(JWTCreationException("dummy", Throwable()), dummyMySession)

            return Stream.of(
                TestCase(
                    "UseCase:成功(RegisteredUser)を返し、MySessionのencode:成功(jwt)を帰す場合、201レスポンスを返す",
                    useCaseSuccess.right(),
                    mySessionEncodeSuccess.right(),
                    ResponseEntity<String>("""{"user":{"email":"dummy@example.com","username":"dummy-name","bio":"dummy-bio","image":"dummy-image","token":"success.encoded.token"}}""", HttpStatus.valueOf(201))
                ),
                TestCase(
                    "UseCase:成功(RegisteredUser)、MySessionのencode:失敗(エンコードエラー)の場合、500エラーレスポンスを返す",
                    useCaseSuccess.right(),
                    mySessionEncodeFailed.left(),
                    ResponseEntity<String>("""{"errors":{"body":["予期せぬエラーが発生しました(cause: null)"]}}""", HttpStatus.valueOf(500))
                ),
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    val actual = UserAndAuthenticationController(
                        object : MySessionJwt { override fun encode(session: MySession): Either<MySessionJwt.EncodeError, String> = testCase.mySessionEncodeResult },
                        object : MyAuth {}, // 関係ない
                        object :
                            RegisterUserUseCase { override fun execute(email: String?, password: String?, username: String?): Either<RegisterUserUseCase.Error, RegisteredUser> = testCase.useCaseExecuteResult },
                        object : LoginUseCase {}, // 関係ない
                        object : UpdateUserUseCase {}, // 関係ない
                    ).register("""{"user": {}}""")

                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }
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
            UserAndAuthenticationController(
                mySessionJwtEncodeReturnString,
                notImplementedMyAuth,
                registerUserUseCase,
                notImplementedLoginUseCase,
                object : UpdateUserUseCase {}, // 関係ない
            )
        @Test
        fun `UseCase が「RegisteredUser」を返し、JWTエンコードが成功する場合、201レスポンスを返す`() {
            val dummyRegisteredUser = RegisteredUser.newWithoutValidation(
                UserId(1),
                Email.newWithoutValidation("dummy@example.com"),
                Username.newWithoutValidation("dummy-name"),
                Bio.newWithoutValidation("dummy-bio"),
                Image.newWithoutValidation("dummy-image"),
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

    @Nested
    class `Update(ユーザー情報の更新)` {
        private val currentUser =
            RegisteredUser.newWithoutValidation(
                UserId(1),
                Email.newWithoutValidation("dummy@example.com"),
                Username.newWithoutValidation("dummy-username"),
                Bio.newWithoutValidation("dummy-bio"),
                Image.newWithoutValidation("dummy-image"),
            )
        private val myAuthReturnCurrentUser = object : MyAuth {
            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> =
                currentUser.right()
        }
        private val jwtEncodeSessionReturnSuccess = object : MySessionJwt {
            override fun encode(session: MySession) = "dummy-jwt-token".right()
        }
        private fun newUserAndAuthenticationController(returnValue: Either<UpdateUserUseCase.Error, RegisteredUser>): UserAndAuthenticationController =
            UserAndAuthenticationController(
                jwtEncodeSessionReturnSuccess,
                myAuthReturnCurrentUser,
                object : RegisterUserUseCase {},
                object : LoginUseCase {},
                object : UpdateUserUseCase {
                    override fun execute(
                        currentUser: RegisteredUser,
                        email: String?,
                        username: String?,
                        bio: String?,
                        image: String?
                    ): Either<UpdateUserUseCase.Error, RegisteredUser> = returnValue
                },
            )
        @Test
        fun `UseCase から "更新後のユーザー" を返し、セッションのエンコードに成功した場合、 200 レスポンスを返す`() {
            val updatedUser = RegisteredUser.newWithoutValidation(
                UserId(1),
                Email.newWithoutValidation("new-dummy@example.com"),
                Username.newWithoutValidation("dummy-username"),
                Bio.newWithoutValidation("dummy-bio"),
                Image.newWithoutValidation("dummy-image"),
            )
            val controller = newUserAndAuthenticationController(updatedUser.right())

            val actual = controller.update("dummy", """{"user":{}}""")
            val expected = ResponseEntity(
                """{"user":{"email":"new-dummy@example.com","username":"dummy-username","bio":"dummy-bio","image":"dummy-image","token":"dummy-jwt-token"}}""",
                HttpStatus.valueOf(200)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `UseCase から "ユーザー情報が不正である" 旨のエラーが返ってきた場合、 422 レスポンスを返す`() {
            val invalidAttributesForUpdateUser = UpdateUserUseCase.Error.InvalidAttributesForUpdateUser(
                currentUser,
                nonEmptyListOf(object : MyError.ValidationError {
                    override val key: String get() = "dummy-key"
                    override val message: String get() = "dummy-message"
                })
            )
            val controller = newUserAndAuthenticationController(invalidAttributesForUpdateUser.left())

            val actual = controller.update("dummy", """{"user":{}}""")
            val expected = ResponseEntity(
                """{"errors":{"body":[{"key":"dummy-key","message":"dummy-message"}]}}""",
                HttpStatus.valueOf(422)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `UseCase から "更新するべき情報が無い" 旨のエラーが返ってきた場合、 422 レスポンスを返す`() {
            val noChangeError = UpdateUserUseCase.Error.NoChange(currentUser)
            val controller = newUserAndAuthenticationController(noChangeError.left())

            val actual = controller.update("dummy", """{"user":{}}""")
            val expected = ResponseEntity(
                """更新する項目がありません""",
                HttpStatus.valueOf(422)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `UseCase から "原因不明である" 旨のエラーが返ってきた場合、 500 レスポンスを返す`() {
            val unexpectedError = UpdateUserUseCase.Error.Unexpected(
                currentUser,
                object : MyError {}
            )
            val controller = newUserAndAuthenticationController(unexpectedError.left())

            val actual = controller.update("dummy", """{"user":{}}""")
            val expected = ResponseEntity(
                """{"errors":{"body":["原因不明のエラーが発生しました"]}}""",
                HttpStatus.valueOf(500)
            )
            assertThat(actual).isEqualTo(expected)
        }
    }
}
