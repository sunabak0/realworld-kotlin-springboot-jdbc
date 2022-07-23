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
        private fun createUserAndAuthenticationController(loginUseCaseResult: Either<LoginUseCase.Error, RegisteredUser>): UserAndAuthenticationController =
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
                    override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> = authorizeResult
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

    @Nested
    @DisplayName("ユーザー情報の更新")
    class UpdateTest {
        private data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<UpdateUserUseCase.Error, RegisteredUser>,
            val expected: ResponseEntity<String>
        )
        /**
         * ユーザー情報の更新UseCase の戻り値を固定した Controller を作成
         *
         * 認証は必ず '成功' する
         * JWTエンコーディングは必ず '成功' する
         *
         * @param[updateUserUseCaseResult] UseCaseの実行の戻り値となる値
         * @return 引数を戻り値とする login が実装された Controller
         */
        private fun createUserAndAuthenticationController(updateUserUseCaseResult: Either<UpdateUserUseCase.Error, RegisteredUser>): UserAndAuthenticationController =
            UserAndAuthenticationController(
                mySessionJwt = object : MySessionJwt {
                    override fun encode(session: MySession) = "dummy-jwt-token".right()
                },
                myAuth = object : MyAuth {
                    override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> = RegisteredUser.newWithoutValidation(
                        userId = UserId(1),
                        email = Email.newWithoutValidation("dummy@example.com"),
                        username = Username.newWithoutValidation("dummy-username"),
                        bio = Bio.newWithoutValidation("dummy-bio"),
                        image = Image.newWithoutValidation("dummy-image")
                    ).right()
                },
                registerUserUseCase = object : RegisterUserUseCase {}, // 関係ない
                loginUseCase = object : LoginUseCase {}, // 関係ない
                updateUserUseCase = object : UpdateUserUseCase {
                    override fun execute(
                        currentUser: RegisteredUser,
                        email: String?,
                        username: String?,
                        bio: String?,
                        image: String?
                    ): Either<UpdateUserUseCase.Error, RegisteredUser> = updateUserUseCaseResult
                }
            )

        @TestFactory
        fun test(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    title = "成功: UseCase の実行結果が '登録されたユーザー' の場合、 200 レスポンスを返す",
                    useCaseExecuteResult = RegisteredUser.newWithoutValidation(
                        userId = UserId(1),
                        email = Email.newWithoutValidation("dummy@example.com"),
                        username = Username.newWithoutValidation("dummy-username"),
                        bio = Bio.newWithoutValidation("dummy-bio"),
                        image = Image.newWithoutValidation("dummy-image")
                    ).right(),
                    expected = ResponseEntity(
                        """{"user":{"email":"dummy@example.com","username":"dummy-username","bio":"dummy-bio","image":"dummy-image","token":"dummy-jwt-token"}}""",
                        HttpStatus.valueOf(200)
                    )
                ),
                TestCase(
                    title = "失敗: UseCase の実行結果が '更新しようとしたプロパティが不正である' 旨のエラーの場合、 422 エラーレスポンスを返す",
                    useCaseExecuteResult = UpdateUserUseCase.Error.InvalidAttributesForUpdateUser(
                        currentUser = RegisteredUser.newWithoutValidation(
                            userId = UserId(1),
                            email = Email.newWithoutValidation("dummy@example.com"),
                            username = Username.newWithoutValidation("dummy-username"),
                            bio = Bio.newWithoutValidation("dummy-bio"),
                            image = Image.newWithoutValidation("dummy-image")
                        ),
                        errors = nonEmptyListOf(
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
                    title = "失敗: UseCase の実行結果が '元々のユーザー情報から更新するべき項目' 旨のエラーの場合、 422 エラーレスポンスを返す",
                    useCaseExecuteResult = UpdateUserUseCase.Error.NoChange(
                        currentUser = RegisteredUser.newWithoutValidation(
                            userId = UserId(1),
                            email = Email.newWithoutValidation("dummy@example.com"),
                            username = Username.newWithoutValidation("dummy-username"),
                            bio = Bio.newWithoutValidation("dummy-bio"),
                            image = Image.newWithoutValidation("dummy-image")
                        )
                    ).left(),
                    expected = ResponseEntity(
                        "更新する項目がありません",
                        HttpStatus.valueOf(422)
                    )
                ),
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    // given:
                    val controller = createUserAndAuthenticationController(testCase.useCaseExecuteResult)

                    // when:
                    val actual = controller.update(
                        rawAuthorizationHeader = "Authorization: Bearer dummy.dummy.dummy",
                        rawRequestBody = """{"user": {}}"""
                    )

                    // then:
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }
}
