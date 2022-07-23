package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.Option
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.usecase.profile.FollowProfileUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.profile.ShowProfileUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.profile.UnfollowProfileUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.stream.Stream

class ProfileControllerTest {
    @Nested
    class ShowProfile {
        val dummyRegisteredUser = RegisteredUser.newWithoutValidation(
            UserId(1),
            Email.newWithoutValidation("dummy@example.com"),
            Username.newWithoutValidation("dummy-name"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
        )

        private fun profileController(
            myAuth: MyAuth,
            showProfileUseCase: ShowProfileUseCase,
            followProfileUseCase: FollowProfileUseCase,
            unfollowProfileUseCase: UnfollowProfileUseCase,
        ): ProfileController =
            ProfileController(myAuth, showProfileUseCase, followProfileUseCase, unfollowProfileUseCase)

        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<ShowProfileUseCase.Error, OtherUser>,
            val expected: ResponseEntity<String>,
        )

        @TestFactory
        fun showProfileTest(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    "UseCase:成功（OtherUser）を返す場合、200 レスポンスを返す",
                    OtherUser.newWithoutValidation(
                        UserId(1),
                        Username.newWithoutValidation("hoge-username"),
                        Bio.newWithoutValidation("hoge-bio"),
                        Image.newWithoutValidation("hoge-image"),
                        true,
                    ).right(),
                    ResponseEntity<String>(
                        """{"profile":{"username":"hoge-username","bio":"hoge-bio","image":"hoge-image","following":true}}""",
                        HttpStatus.valueOf(200),
                    ),
                ),
                TestCase(
                    "UseCase:失敗（ValidationError）を返す場合、404 レスポンスを返す",
                    ShowProfileUseCase.Error.InvalidUsername(
                        listOf(object : MyError.ValidationError {
                            override val message: String get() = "DummyValidationError InvalidUsername"
                            override val key: String get() = "DummyKey"
                        })
                    ).left(),
                    ResponseEntity(
                        """{"errors":{"body":["プロフィールが見つかりませんでした"]}}""",
                        HttpStatus.valueOf(404)
                    ),
                ),
                TestCase(
                    "UseCase:失敗（ValidationError）を返す場合、404 レスポンスを返す",
                    ShowProfileUseCase.Error.NotFound(object : MyError {}).left(),
                    ResponseEntity(
                        """{"errors":{"body":["プロフィールが見つかりませんでした"]}}""",
                        HttpStatus.valueOf(404)
                    ),
                ),
                TestCase(
                    "UseCase:失敗（Unexpected）を返す場合、500 レスポンスを返す",
                    ShowProfileUseCase.Error.Unexpected(object : MyError {}).left(),
                    ResponseEntity(
                        """{"errors":{"body":["原因不明のエラーが発生しました"]}}""",
                        HttpStatus.valueOf(500)
                    ),
                )
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    val actual = profileController(
                        object : MyAuth {
                            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                                return dummyRegisteredUser.right()
                            }
                        },
                        object : ShowProfileUseCase {
                            override fun execute(
                                username: String?,
                                currentUser: Option<RegisteredUser>
                            ): Either<ShowProfileUseCase.Error, OtherUser> =
                                testCase.useCaseExecuteResult
                        },
                        object : FollowProfileUseCase {},
                        object : UnfollowProfileUseCase {}
                    ).showProfile(rawAuthorizationHeader = "hoge-authorize", username = "hoge-username")

                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }

    @Nested
    class Follow {
        val dummyRegisteredUser = RegisteredUser.newWithoutValidation(
            UserId(1),
            Email.newWithoutValidation("dummy@example.com"),
            Username.newWithoutValidation("dummy-name"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
        )

        private fun profileController(
            myAuth: MyAuth,
            showProfileUseCase: ShowProfileUseCase,
            followProfileUseCase: FollowProfileUseCase,
            unfollowProfileUseCase: UnfollowProfileUseCase,
        ): ProfileController =
            ProfileController(myAuth, showProfileUseCase, followProfileUseCase, unfollowProfileUseCase)

        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<FollowProfileUseCase.Error, OtherUser>,
            val expected: ResponseEntity<String>,
        )

        @TestFactory
        fun followTest(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    "UseCase:成功（OtherUser）を返す場合、200 レスポンスを返す",
                    OtherUser.newWithoutValidation(
                        UserId(1),
                        Username.newWithoutValidation("hoge-username"),
                        Bio.newWithoutValidation("hoge-bio"),
                        Image.newWithoutValidation("hoge-image"),
                        true,
                    ).right(),
                    ResponseEntity<String>(
                        """{"profile":{"username":"hoge-username","bio":"hoge-bio","image":"hoge-image","following":true}}""",
                        HttpStatus.valueOf(200)
                    )
                ),
                TestCase(
                    "UseCase:失敗（ValidationError）を返す場合、404 レスポンスを返す",
                    FollowProfileUseCase.Error.InvalidUsername(
                        listOf(object : MyError.ValidationError {
                            override val message: String get() = "DummyValidationError InvalidUsername"
                            override val key: String get() = "DummyKey"
                        })
                    ).left(),
                    ResponseEntity(
                        """{"errors":{"body":["プロフィールが見つかりませんでした"]}}""",
                        HttpStatus.valueOf(404)
                    )
                ),
                TestCase(
                    "UseCase:失敗（NotFound)を返す場合、404 レスポンスを返す",
                    FollowProfileUseCase.Error.NotFound(object : MyError {}).left(),
                    ResponseEntity(
                        """{"errors":{"body":["プロフィールが見つかりませんでした"]}}""",
                        HttpStatus.valueOf(404)
                    )
                ),
                TestCase(
                    "UseCase:失敗（Unexpected）を返す場合、500 レスポンスを返す",
                    FollowProfileUseCase.Error.Unexpected(object : MyError {}).left(),
                    ResponseEntity(
                        """{"errors":{"body":["原因不明のエラーが発生しました"]}}""",
                        HttpStatus.valueOf(500)
                    )
                )
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    val actual = profileController(
                        object : MyAuth {
                            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                                return dummyRegisteredUser.right()
                            }
                        },
                        object : ShowProfileUseCase {},
                        object : FollowProfileUseCase {
                            override fun execute(
                                username: String?,
                                currentUser: RegisteredUser
                            ): Either<FollowProfileUseCase.Error, OtherUser> =
                                testCase.useCaseExecuteResult
                        },
                        object : UnfollowProfileUseCase {}
                    ).follow(rawAuthorizationHeader = "hoge-authorize", username = "hoge-username")

                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }

    @Nested
    class Unfollow {
        val dummyRegisteredUser = RegisteredUser.newWithoutValidation(
            UserId(1),
            Email.newWithoutValidation("dummy@example.com"),
            Username.newWithoutValidation("dummy-name"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
        )

        private fun profileController(
            myAuth: MyAuth,
            showProfileUseCase: ShowProfileUseCase,
            followProfileUseCase: FollowProfileUseCase,
            unfollowProfileUseCase: UnfollowProfileUseCase
        ): ProfileController =
            ProfileController(myAuth, showProfileUseCase, followProfileUseCase, unfollowProfileUseCase)

        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<UnfollowProfileUseCase.Error, OtherUser>,
            val expected: ResponseEntity<String>,
        )

        @TestFactory
        fun unfollowTest(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    "UseCase:成功（OtherUser）を返す場合、200 レスポンスを返す",
                    OtherUser.newWithoutValidation(
                        UserId(1),
                        Username.newWithoutValidation("hoge-username"),
                        Bio.newWithoutValidation("hoge-bio"),
                        Image.newWithoutValidation("hoge-image"),
                        false,
                    ).right(),
                    ResponseEntity(
                        """{"profile":{"username":"hoge-username","bio":"hoge-bio","image":"hoge-image","following":false}}""",
                        HttpStatus.valueOf(200)
                    )
                ),
                TestCase(
                    "UseCase:失敗（ValidationError）を返す場合、404 レスポンスを返す",
                    UnfollowProfileUseCase.Error.InvalidUsername(
                        listOf(object : MyError.ValidationError {
                            override val message: String get() = "DummyValidationError InvalidUsername"
                            override val key: String get() = "DummyKey"
                        })
                    ).left(),
                    ResponseEntity(
                        """{"errors":{"body":["プロフィールが見つかりませんでした"]}}""",
                        HttpStatus.valueOf(404)
                    ),
                ),
                TestCase(
                    "UseCase:失敗（NotFound）を返す場合、404 レスポンスを返す",
                    UnfollowProfileUseCase.Error.NotFound(object : MyError {}).left(),
                    ResponseEntity(
                        """{"errors":{"body":["プロフィールが見つかりませんでした"]}}""",
                        HttpStatus.valueOf(404)
                    ),
                ),
                TestCase(
                    "UseCase:失敗（Unexpected）を返す場合、500 レスポンスを返す",
                    UnfollowProfileUseCase.Error.Unexpected(object : MyError {}).left(),
                    ResponseEntity(
                        """{"errors":{"body":["原因不明のエラーが発生しました"]}}""",
                        HttpStatus.valueOf(500)
                    )
                )
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    val actual = profileController(
                        object : MyAuth {
                            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                                return dummyRegisteredUser.right()
                            }
                        },
                        object : ShowProfileUseCase {},
                        object : FollowProfileUseCase {},
                        object : UnfollowProfileUseCase {
                            override fun execute(
                                username: String?,
                                currentUser: RegisteredUser
                            ): Either<UnfollowProfileUseCase.Error, OtherUser> =
                                testCase.useCaseExecuteResult
                        }
                    ).unfollow(rawAuthorizationHeader = "hoge-authorize", username = "hoge-username")

                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }
}
