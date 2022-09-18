package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UnregisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UpdatableRegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.Password
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.infra.helper.SeedData
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
import org.junit.jupiter.api.TestFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.Date
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
        private fun createUserAndAuthenticationController(
            registerUserUseCaseResult: Either<RegisterUserUseCase.Error, RegisteredUser>
        ): UserAndAuthenticationController =
            UserAndAuthenticationController(
                mySessionJwt = object : MySessionJwt {
                    override fun encode(session: MySession) = "dummy-jwt-token".right()
                },
                myAuth = object : MyAuth {}, // 関係ない
                registerUserUseCase = object : RegisterUserUseCase {
                    override fun execute(
                        email: String?,
                        password: String?,
                        username: String?,
                    ): Either<RegisterUserUseCase.Error, RegisteredUser> = registerUserUseCaseResult
                },
                loginUseCase = object : LoginUseCase {}, // 関係ない
                updateUserUseCase = object : UpdateUserUseCase {}, // 関係ない
            )

        @TestFactory
        fun test(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    title = "成功: UseCase の実行結果が '登録されたユーザー' の場合、 201 レスポンスを返す",
                    useCaseExecuteResult = RegisteredUser.newWithoutValidation(
                        userId = UserId(1),
                        email = Email.newWithoutValidation("dummy@example.com"),
                        username = Username.newWithoutValidation("dummy-username"),
                        bio = Bio.newWithoutValidation("dummy-bio"),
                        image = Image.newWithoutValidation("dummy-image")
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
                        cause = object : MyError {},
                        user = object : UnregisteredUser {
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
                        cause = object : MyError {},
                        user = object : UnregisteredUser {
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
    @DisplayName("ログイン")
    class LoginTest {
        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<LoginUseCase.Error, RegisteredUser>,
            val expected: ResponseEntity<String>
        )

        /**
         * ログインUseCase の戻り値を固定した Controller を作成
         *
         * JWTエンコーディングは必ず '成功' する
         *
         * @param[loginUseCaseResult] UseCaseの実行の戻り値となる値
         * @return 引数を戻り値とする login が実装された Controller
         */
        private fun createUserAndAuthenticationController(
            loginUseCaseResult: Either<LoginUseCase.Error, RegisteredUser>
        ): UserAndAuthenticationController =
            UserAndAuthenticationController(
                mySessionJwt = object : MySessionJwt {
                    override fun encode(session: MySession) = "dummy-jwt-token".right()
                },
                myAuth = object : MyAuth {}, // 関係ない
                registerUserUseCase = object : RegisterUserUseCase {}, // 関係ない
                loginUseCase = object : LoginUseCase {
                    override fun execute(
                        email: String?,
                        password: String?
                    ): Either<LoginUseCase.Error, RegisteredUser> = loginUseCaseResult
                },
                updateUserUseCase = object : UpdateUserUseCase {}, // 関係ない
            )

        @TestFactory
        fun test(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    title = "成功: UseCase の実行結果が '登録されたユーザー' の場合、 201 レスポンスを返す",
                    useCaseExecuteResult = RegisteredUser.newWithoutValidation(
                        userId = UserId(1),
                        email = Email.newWithoutValidation("dummy@example.com"),
                        username = Username.newWithoutValidation("dummy-username"),
                        bio = Bio.newWithoutValidation("dummy-bio"),
                        image = Image.newWithoutValidation("dummy-image")
                    ).right(),
                    expected = ResponseEntity(
                        """{"user":{"email":"dummy@example.com","username":"dummy-username","bio":"dummy-bio","image":"dummy-image","token":"dummy-jwt-token"}}""",
                        HttpStatus.valueOf(201)
                    )
                ),
                TestCase(
                    title = "失敗: UseCase の実行結果が 'Emailかパスワードが不正である' 旨のエラーの場合、 401 レスポンスを返す",
                    useCaseExecuteResult = LoginUseCase.Error.InvalidEmailOrPassword(
                        listOf(
                            object : MyError.ValidationError {
                                override val message: String get() = "dummy-原因"
                                override val key: String get() = "dummy-プロパティ名"
                            }
                        )
                    ).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":[{"key":"dummy-プロパティ名","message":"dummy-原因"}]}}""",
                        HttpStatus.valueOf(401)
                    )
                ),
                TestCase(
                    title = "失敗: UseCase の実行結果が '認証できなかった' 旨のエラーの場合、 401 レスポンスを返す",
                    useCaseExecuteResult = LoginUseCase.Error.Unauthorized(
                        Email.newWithoutValidation("dummy@example.com")
                    ).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":["認証に失敗しました"]}}""",
                        HttpStatus.valueOf(401)
                    )
                ),
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    // given:
                    val controller = createUserAndAuthenticationController(testCase.useCaseExecuteResult)

                    // when:
                    val actual = controller.login("""{"user": {}}""")

                    // then:
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }

    @Nested
    @DisplayName("(ログイン済みである)自分自身のユーザー情報取得")
    class ShowCurrentUserTest {
        data class TestCase(
            val title: String,
            val authorizeResult: Either<MyAuth.Unauthorized, RegisteredUser>,
            val expected: ResponseEntity<String>
        )

        /**
         * MyAuthのauthorize の戻り値を固定した MyAuth を作成
         *
         * JWTエンコーディングは必ず '成功' する
         *
         * @param[authorizeResult] UseCaseの実行の戻り値となる値
         * @return 引数を戻り値とする login が実装された Controller
         */
        private fun createUserAndAuthenticationController(authorizeResult: Either<MyAuth.Unauthorized, RegisteredUser>): UserAndAuthenticationController =
            UserAndAuthenticationController(
                mySessionJwt = object : MySessionJwt {
                    override fun encode(session: MySession) = "dummy-jwt-token".right()
                },
                myAuth = object : MyAuth {
                    override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> =
                        authorizeResult
                },
                registerUserUseCase = object : RegisterUserUseCase {}, // 関係ない
                loginUseCase = object : LoginUseCase {}, // 関係ない
                updateUserUseCase = object : UpdateUserUseCase {}, // 関係ない
            )

        @TestFactory
        fun test(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    title = "成功: authorize の実行結果が '登録されたユーザー' の場合、 201 レスポンスを返す",
                    authorizeResult = RegisteredUser.newWithoutValidation(
                        userId = UserId(1),
                        email = Email.newWithoutValidation("dummy@example.com"),
                        username = Username.newWithoutValidation("dummy-username"),
                        bio = Bio.newWithoutValidation("dummy-bio"),
                        image = Image.newWithoutValidation("dummy-image")
                    ).right(),
                    expected = ResponseEntity(
                        """{"user":{"email":"dummy@example.com","username":"dummy-username","bio":"dummy-bio","image":"dummy-image","token":"dummy-jwt-token"}}""",
                        HttpStatus.valueOf(201)
                    )
                ),
                TestCase(
                    title = "失敗: authorize の実行結果が 'BearerTokenが無い' 旨のエラーの場合、 401 レスポンスを返す",
                    authorizeResult = MyAuth.Unauthorized.RequiredBearerToken.left(),
                    expected = ResponseEntity(
                        "",
                        HttpStatus.valueOf(401)
                    )
                ),
                TestCase(
                    title = "失敗: authorize の実行結果が 'BearerTokenのパースに失敗した' 旨のエラーの場合、 401 レスポンスを返す",
                    authorizeResult = MyAuth.Unauthorized.FailedParseBearerToken(
                        cause = Throwable("dummy-例外"),
                        authorizationHeader = "Authorization: Bearer dummy.dummy.dummy"
                    ).left(),
                    expected = ResponseEntity(
                        "",
                        HttpStatus.valueOf(401)
                    )
                ),
                TestCase(
                    title = "失敗: authorize の実行結果が 'Decodeに失敗した' 旨のエラーの場合、 401 レスポンスを返す",
                    authorizeResult = MyAuth.Unauthorized.FailedDecodeToken(
                        cause = object : MyError {},
                        token = "dummy.dummy.dummy"
                    ).left(),
                    expected = ResponseEntity(
                        "",
                        HttpStatus.valueOf(401)
                    )
                ),
                TestCase(
                    title = "失敗: authorize の実行結果が 'ユーザーが見つからなかった' 旨のエラーの場合、 401 レスポンスを返す",
                    authorizeResult = MyAuth.Unauthorized.NotFound(
                        cause = object : MyError {},
                        id = UserId(1)
                    ).left(),
                    expected = ResponseEntity(
                        "",
                        HttpStatus.valueOf(401)
                    )
                ),
                TestCase(
                    title = "失敗: authorize の実行結果が 'Emailが変わり、最新のEmailと異なる' 旨のエラーの場合、 401 レスポンスを返す",
                    authorizeResult = MyAuth.Unauthorized.NotMatchEmail(
                        oldEmail = Email.newWithoutValidation("dummy@example.com"),
                        newEmail = Email.newWithoutValidation("changed-dummy@example.com")
                    ).left(),
                    expected = ResponseEntity(
                        "",
                        HttpStatus.valueOf(401)
                    )
                ),
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    // given:
                    val controller = createUserAndAuthenticationController(testCase.authorizeResult)

                    // when:
                    val actual = controller.showCurrentUser("Authorization: Bearer dummy.dummy.dummy")

                    // then:
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }

    @DisplayName("ユーザー情報の更新")
    class Update {
        private data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<UpdateUserUseCase.Error, RegisteredUser>,
            val expected: ResponseEntity<String>
        )
        private val fakeUpdatableRegisteredUser = object : UpdatableRegisteredUser {
            override val userId: UserId get() = throw UnsupportedOperationException()
            override val email: Email get() = throw UnsupportedOperationException()
            override val username: Username get() = throw UnsupportedOperationException()
            override val bio: Bio get() = throw UnsupportedOperationException()
            override val image: Image get() = throw UnsupportedOperationException()
            override val updatedAt: Date get() = throw UnsupportedOperationException()
        }

        @TestFactory
        fun test(): Stream<DynamicNode> = Stream.of(
            TestCase(
                title = "正常系-更新に成功した場合、ステータスコードが200のレスポンス",
                useCaseExecuteResult = SeedData.users().find { it.userId.value == 1 }!!.right(),
                expected = ResponseEntity(
                    """{"user":{"email":"paul-graham@example.com","username":"paul-graham","bio":"Lisper","image":"","token":"dummy-jwt-token"}}""",
                    HttpStatus.valueOf(200)
                )
            ),
            TestCase(
                title = "準正常系-要素が異常だったことが原因で更新に失敗した場合、ステータスコードが400のレスポンス",
                useCaseExecuteResult = UpdateUserUseCase.Error.InvalidAttributes(
                    errors = nonEmptyListOf(
                        UpdatableRegisteredUser.ValidationError.NothingAttributeToUpdatable
                    ),
                    currentUser = SeedData.users().find { it.userId.value == 1 }!!
                ).left(),
                expected = ResponseEntity(
                    """{"errors":{"body":[{"key":"UpdatableRegisteredUser","message":"更新するプロパティが有りません"}]}}""",
                    HttpStatus.valueOf(400)
                )
            ),
            TestCase(
                title = "準正常系-emailが既に利用されていることが原因で更新に失敗した場合、ステータスコードが422のレスポンス",
                useCaseExecuteResult = UpdateUserUseCase.Error.AlreadyUsedEmail(
                    cause = UserRepository.UpdateError.AlreadyRegisteredEmail(email = Email.newWithoutValidation("fake-already-used@example.com")),
                    updatableRegisteredUser = fakeUpdatableRegisteredUser,
                ).left(),
                expected = ResponseEntity(
                    """{"errors":{"body":["メールアドレスは既に登録されています"]}}""",
                    HttpStatus.valueOf(422)
                )
            ),
            TestCase(
                title = "準正常系-usernameが既に利用されていることが原因で更新に失敗した場合、ステータスコードが422のレスポンス",
                useCaseExecuteResult = UpdateUserUseCase.Error.AlreadyUsedUsername(
                    cause = UserRepository.UpdateError.AlreadyRegisteredUsername(username = Username.newWithoutValidation("fake-already-used-username")),
                    updatableRegisteredUser = fakeUpdatableRegisteredUser,
                ).left(),
                expected = ResponseEntity(
                    """{"errors":{"body":["ユーザー名は既に登録されています"]}}""",
                    HttpStatus.valueOf(422)
                )
            ),
            TestCase(
                title = "準正常系-ユーザーが見つからなかったことが原因で更新に失敗した場合、ステータスコードが422のレスポンス",
                useCaseExecuteResult = UpdateUserUseCase.Error.NotFoundUser(
                    cause = UserRepository.UpdateError.NotFound(userId = UserId(1)),
                    currentUser = SeedData.users().find { it.userId.value == 1 }!!
                ).left(),
                expected = ResponseEntity(
                    """{"errors":{"body":["ユーザーが見つかりませんでした"]}}""",
                    HttpStatus.valueOf(404)
                )
            ),
        ).map { testCase ->
            dynamicTest(testCase.title) {
                /**
                 * given:
                 */
                val controller = UserAndAuthenticationController(
                    mySessionJwt = object : MySessionJwt {
                        override fun encode(session: MySession) = "dummy-jwt-token".right()
                    },
                    myAuth = object : MyAuth {
                        override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> =
                            SeedData.users().find { it.userId.value == 1 }!!.right()
                    },
                    registerUserUseCase = object : RegisterUserUseCase {},
                    loginUseCase = object : LoginUseCase {},
                    updateUserUseCase = object : UpdateUserUseCase {
                        override fun execute(
                            currentUser: RegisteredUser,
                            email: String?,
                            username: String?,
                            bio: String?,
                            image: String?
                        ): Either<UpdateUserUseCase.Error, RegisteredUser> = testCase.useCaseExecuteResult
                    }
                )

                /**
                 * when:
                 */
                val actual = controller.update(
                    rawAuthorizationHeader = "Authorization: Bearer fake.fake.fake",
                    rawRequestBody = """{"user": {}}"""
                )

                /**
                 * then:
                 */
                assertThat(actual).isEqualTo(testCase.expected)
            }
        }
    }
}
