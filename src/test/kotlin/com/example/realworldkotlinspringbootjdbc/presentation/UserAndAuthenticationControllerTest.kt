package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.stream.Stream

class UserAndAuthenticationControllerTest {
    @Nested
    @DisplayName("ユーザー登録")
    class RegisterTest {
        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<RegisterUserUseCase.Error, RegisteredUser>,
            val expected: ResponseEntity<String>
        )

        /**
         * ユーザー登録UseCase の戻り値を固定した Controller を作成
         *
         * JWTエンコーディングは必ず '成功' する
         *
         * @param[registerUserUseCaseResult] UseCaseの実行の戻り値となる値
         * @return 引数を戻り値とする register が実装された Controller
         */
        private fun createUserAndAuthenticationController(registerUserUseCaseResult: Either<RegisterUserUseCase.Error, RegisteredUser>): UserAndAuthenticationController =
            UserAndAuthenticationController(
                object : MySessionJwt {
                    override fun encode(session: MySession) = "dummy-jwt-token".right()
                },
                object : MyAuth {}, // 関係ない
                object : RegisterUserUseCase {
                    override fun execute(
                        email: String?,
                        password: String?,
                        username: String?,
                    ): Either<RegisterUserUseCase.Error, RegisteredUser> = registerUserUseCaseResult
                },
                object : LoginUseCase {}, // 関係ない
                object : UpdateUserUseCase {}, // 関係ない
            )

        @TestFactory
        fun test(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    title = "成功: UseCase の実行結果が '登録されたユーザー' の場合、 201 レスポンスを返す",
                    useCaseExecuteResult = RegisteredUser.newWithoutValidation(
                        UserId(1),
                        Email.newWithoutValidation("dummy@example.com"),
                        Username.newWithoutValidation("dummy-username"),
                        Bio.newWithoutValidation("dummy-bio"),
                        Image.newWithoutValidation("dummy-image")
                    ).right(),
                    expected = ResponseEntity(
                        """{"user":{"email":"dummy@example.com","username":"dummy-username","bio":"dummy-bio","image":"dummy-image","token":"dummy-jwt-token"}}""",
                        HttpStatus.valueOf(201)
                    )
                ),
                TestCase(
                    title = "失敗: UseCase の実行結果が 'プロパティが不正である' 旨のエラーの場合、 422 エラーレスポンスを返す",
                    useCaseExecuteResult = RegisterUserUseCase.Error.InvalidUser(
                        listOf(
                            object : MyError.ValidationError {
                                override val message: String get() = "dummy-原因"
                                override val key: String get() = "dummy-プロパティ名"
                            }
                        )
                    ).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":[{"key":"dummy-プロパティ名","message":"dummy-原因"}]}}""",
                        HttpStatus.valueOf(422)
                    )
                ),
                TestCase(
                    title = "失敗: UseCase の実行結果が 'Emailが既に登録されている' 旨のエラーの場合、 422 エラーレスポンスを返す",
                    useCaseExecuteResult = RegisterUserUseCase.Error.AlreadyRegisteredEmail(
                        object : MyError {},
                        object : UnregisteredUser {
                            override val email: Email get() = Email.newWithoutValidation("dummy@example.com")
                            override val password: Password get() = Password.newWithoutValidation("dummy-password")
                            override val username: Username get() = Username.newWithoutValidation("dummy-username")
                        }
                    ).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":["メールアドレスは既に登録されています"]}}""",
                        HttpStatus.valueOf(422)
                    )
                ),
                TestCase(
                    title = "失敗: UseCase の実行結果が 'ユーザー名が既に登録されている' 旨のエラーの場合、 422 エラーレスポンスを返す",
                    useCaseExecuteResult = RegisterUserUseCase.Error.AlreadyRegisteredUsername(
                        object : MyError {},
                        object : UnregisteredUser {
                            override val email: Email get() = Email.newWithoutValidation("dummy@example.com")
                            override val password: Password get() = Password.newWithoutValidation("dummy-password")
                            override val username: Username get() = Username.newWithoutValidation("dummy-username")
                        }
                    ).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":["ユーザー名は既に登録されています"]}}""",
                        HttpStatus.valueOf(422)
                    )
                )
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    // given:
                    val controller = createUserAndAuthenticationController(testCase.useCaseExecuteResult)

                    // when:
                    val actual = controller.register("""{"user": {}}""")

                    // then:
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
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
