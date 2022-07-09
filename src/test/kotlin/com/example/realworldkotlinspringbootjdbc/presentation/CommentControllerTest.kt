package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.Option
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.usecase.comment.CreateCommentUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.comment.DeleteCommentUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.comment.ListCommentUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.text.SimpleDateFormat
import java.util.stream.Stream
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body as CommentBody

class CommentControllerTest {
    @Nested
    class ListComment {
        private fun commentController(
            myAuth: MyAuth,
            commentsUseCase: ListCommentUseCase,
            createCommentUseCase: CreateCommentUseCase,
            deleteCommentUseCase: DeleteCommentUseCase
        ): CommentController =
            CommentController(myAuth, commentsUseCase, createCommentUseCase, deleteCommentUseCase)

        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<ListCommentUseCase.Error, List<Comment>>,
            val expected: ResponseEntity<String>
        )

        @TestFactory
        fun listTest(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    "UseCase:成功（List<Comment>）を返す場合、200レスポンスを返す",
                    listOf(
                        Comment.newWithoutValidation(
                            CommentId.newWithoutValidation(1),
                            CommentBody.newWithoutValidation("hoge-body-1"),
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                            OtherUser.newWithoutValidation(
                                UserId(1),
                                Username.newWithoutValidation("hoge-author-1"),
                                Bio.newWithoutValidation("hoge-bio-1"),
                                Image.newWithoutValidation("hoge-image-1"),
                                false,
                            )
                        ),
                        Comment.newWithoutValidation(
                            CommentId.newWithoutValidation(2),
                            CommentBody.newWithoutValidation("hoge-body-2"),
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
                            OtherUser.newWithoutValidation(
                                UserId(1),
                                Username.newWithoutValidation("hoge-author-2"),
                                Bio.newWithoutValidation("hoge-bio-2"),
                                Image.newWithoutValidation("hoge-image-2"),
                                false,
                            )
                        ),
                    ).right(),
                    ResponseEntity(
                        """{"comments":[{"id":1,"body":"hoge-body-1","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","author":"hoge-author-1"},{"id":2,"body":"hoge-body-2","createdAt":"2022-02-01T15:00:00.000Z","updatedAt":"2022-02-01T15:00:00.000Z","author":"hoge-author-2"}]}""",
                        HttpStatus.valueOf(200)
                    )
                ),
                TestCase(
                    "UseCase:失敗（NotFound）を返す場合、404 エラーレスポンスを返す",
                    ListCommentUseCase.Error.NotFound(object : MyError {}).left(),
                    ResponseEntity("""{"errors":{"body":["記事が見つかりませんでした"]}}""", HttpStatus.valueOf(404)),
                ),
                TestCase(
                    "UseCase:失敗（ValidationError）を返す場合、422 エラーレスポンスを返す",
                    ListCommentUseCase.Error.InvalidSlug(
                        listOf(object : MyError.ValidationError {
                            override val message: String get() = "DummyValidationError"
                            override val key: String get() = "DummyKey"
                        })
                    ).left(),
                    ResponseEntity(
                        """{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError"}]}}""",
                        HttpStatus.valueOf(422)
                    ),
                ),
                TestCase(
                    "UseCase:失敗（Unexpected）を返す場合、500 エラーレスポンスを返す",
                    ListCommentUseCase.Error.Unexpected(object : MyError {}).left(),
                    ResponseEntity("""{"errors":{"body":["原因不明のエラーが発生しました"]}}""", HttpStatus.valueOf(500)),
                )
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    val actual =
                        commentController(
                            object : MyAuth {},
                            object : ListCommentUseCase {
                                override fun execute(slug: String?): Either<ListCommentUseCase.Error, List<Comment>> =
                                    testCase.useCaseExecuteResult
                            },
                            object : CreateCommentUseCase {},
                            object : DeleteCommentUseCase {}
                        ).list(
                            slug = "hoge-slug"
                        )
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }

    @Nested
    class `List(コメント取得) JWT 認証失敗 or 未ログイン` {
        private val requestHeader = "hoge-authorize"
        private val pathParam = "hoge-slug"
        private fun commentController(
            myAuth: MyAuth,
            commentsUseCase: ListCommentUseCase,
            createCommentUseCase: CreateCommentUseCase,
            deleteCommentUseCase: DeleteCommentUseCase
        ): CommentController =
            CommentController(myAuth, commentsUseCase, createCommentUseCase, deleteCommentUseCase)

        private val unauthorizedMyAuth = object : MyAuth {
            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                return MyAuth.Unauthorized.RequiredBearerToken.left()
            }
        }

        private val notImplementedCreateCommentUseCase = object : CreateCommentUseCase {}

        private val notImplementedDeleteCommentUseCase = object : DeleteCommentUseCase {}

        @Test
        fun `JWT 認証失敗 or 未ログイン-コメント取得-UseCase が「Comment」のリストを返す場合、200レスポンスを返す`() {
            val mockComments = listOf(
                Comment.newWithoutValidation(
                    CommentId.newWithoutValidation(1),
                    CommentBody.newWithoutValidation("hoge-body-1"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    OtherUser.newWithoutValidation(
                        UserId(1),
                        Username.newWithoutValidation("hoge-author-1"),
                        Bio.newWithoutValidation("hoge-bio-1"),
                        Image.newWithoutValidation("hoge-image-1"),
                        following = false,
                    )
                ),
                Comment.newWithoutValidation(
                    CommentId.newWithoutValidation(2),
                    CommentBody.newWithoutValidation("hoge-body-2"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
                    OtherUser.newWithoutValidation(
                        UserId(1),
                        Username.newWithoutValidation("hoge-author-1"),
                        Bio.newWithoutValidation("hoge-bio-1"),
                        Image.newWithoutValidation("hoge-image-1"),
                        following = false,
                    )
                ),
            )
            val listReturnComment = object : ListCommentUseCase {
                override fun execute(
                    slug: String?,
                    currentUser: Option<RegisteredUser>
                ): Either<ListCommentUseCase.Error, List<Comment>> =
                    mockComments.right()
            }
            val actual =
                commentController(
                    unauthorizedMyAuth,
                    listReturnComment,
                    notImplementedCreateCommentUseCase,
                    notImplementedDeleteCommentUseCase
                ).list(
                    requestHeader,
                    pathParam
                )
            val expected = ResponseEntity(
                """{"comments":[{"id":1,"body":"hoge-body-1","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","author":"hoge-author-1"},{"id":2,"body":"hoge-body-2","createdAt":"2022-02-01T15:00:00.000Z","updatedAt":"2022-02-01T15:00:00.000Z","author":"hoge-author-1"}]}""",
                HttpStatus.valueOf(200)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `JWT 認証失敗 or 未ログイン-コメント取得-UseCase が「NotFound」を返す場合、404 エラーレスポンスを返す`() {
            val notImplementedError = object : MyError {}
            val listReturnNotFoundError = object : ListCommentUseCase {
                override fun execute(
                    slug: String?,
                    currentUser: Option<RegisteredUser>
                ): Either<ListCommentUseCase.Error, List<Comment>> =
                    ListCommentUseCase.Error.NotFound(notImplementedError).left()
            }
            val actual = commentController(
                unauthorizedMyAuth,
                listReturnNotFoundError,
                notImplementedCreateCommentUseCase,
                notImplementedDeleteCommentUseCase
            ).list(
                requestHeader,
                pathParam
            )
            val expected = ResponseEntity("""{"errors":{"body":["記事が見つかりませんでした"]}}""", HttpStatus.valueOf(404))
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `JWT 認証失敗 or 未ログイン-コメント取得-UseCase が「バリデーションエラー」を返す場合、404 エラーレスポンスを返す`() {
            /**
             * FIXME
             * ローカルでは動作するが、Github Actions で動作しない変数名を一時的に mockE に修正
             * 命名規則の方針が決まり次第修正
             */
            val mockE = object : MyError.ValidationError {
                override val message: String get() = "DummyValidationError"
                override val key: String get() = "DummyKey"
            }

            /**
             * FIXME
             * ローカルでは動作するが、Github Actions で動作しない変数名を一時的に mockUC に修正
             * 命名規則の方針が決まり次第修正
             */
            val mockUC = object : ListCommentUseCase {
                override fun execute(
                    slug: String?,
                    currentUser: Option<RegisteredUser>
                ): Either<ListCommentUseCase.Error, List<Comment>> {
                    return ListCommentUseCase.Error.InvalidSlug(listOf(mockE)).left()
                }
            }
            val actual = commentController(
                unauthorizedMyAuth,
                mockUC,
                notImplementedCreateCommentUseCase,
                notImplementedDeleteCommentUseCase
            ).list(
                requestHeader,
                pathParam
            )
            val expected = ResponseEntity(
                """{"errors":{"body":["記事が見つかりませんでした"]}}""",
                HttpStatus.valueOf(404)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `JWT 認証失敗 or 未ログイン-コメント取得-UseCase が原因不明のエラーを返す場合、500 エラーレスポンスを返す`() {
            /**
             * FIXME
             * ローカルでは動作するが、Github Actions で動作しない変数名を一時的に mockE に修正
             * 命名規則の方針が決まり次第修正
             */
            val mockE = object : MyError {}

            /**
             * FIXME
             * ローカルでは動作するが、Github Actions で動作しない変数名を一時的に mockUE に修正
             * 命名規則の方針が決まり次第修正
             */
            val mockUE = object : ListCommentUseCase {
                override fun execute(
                    slug: String?,
                    currentUser: Option<RegisteredUser>
                ): Either<ListCommentUseCase.Error, List<Comment>> {
                    return ListCommentUseCase.Error.Unexpected(mockE).left()
                }
            }
            val actual = commentController(
                unauthorizedMyAuth,
                mockUE,
                notImplementedCreateCommentUseCase,
                notImplementedDeleteCommentUseCase
            ).list(
                requestHeader,
                pathParam
            )
            val expected = ResponseEntity("""{"errors":{"body":["原因不明のエラーが発生しました"]}}""", HttpStatus.valueOf(500))
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    class Create {
        private val requestHeader = "hoge-authorize"
        private val pathParam = "hoge-slug"
        private val requestBody = """
                {
                    "comment": {
                        "body": "hoge-body"
                    }
                }
        """.trimIndent()
        val dummyRegisteredUser = RegisteredUser.newWithoutValidation(
            UserId(1),
            Email.newWithoutValidation("dummy@example.com"),
            Username.newWithoutValidation("dummy-name"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
        )

        private fun commentController(
            myAuth: MyAuth,
            listCommentUseCase: ListCommentUseCase,
            createCommentUseCase: CreateCommentUseCase,
            deleteCommentUseCase: DeleteCommentUseCase
        ): CommentController =
            CommentController(myAuth, listCommentUseCase, createCommentUseCase, deleteCommentUseCase)

        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<CreateCommentUseCase.Error, Comment>,
            val expected: ResponseEntity<String>,
        )

        @TestFactory
        fun createTest(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    "UseCase:成功（作成したComment）を返す場合、200 レスポンスを返す",
                    Comment.newWithoutValidation(
                        CommentId.newWithoutValidation(1),
                        CommentBody.newWithoutValidation("hoge-body"),
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                        OtherUser.newWithoutValidation(
                            UserId(1),
                            Username.newWithoutValidation("hoge-username"),
                            Bio.newWithoutValidation(""),
                            Image.newWithoutValidation(""),
                            following = true,
                        )
                    ).right(),
                    ResponseEntity(
                        """{"Comment":{"id":1,"body":"hoge-body","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","author":"hoge-username"}}""",
                        HttpStatus.valueOf(200)
                    )
                ),
                TestCase(
                    "UseCase:失敗（Slug が不正によるValidationError）を返す場合、422 エラーレスポンスを返す",
                    CreateCommentUseCase.Error.InvalidSlug(
                        listOf(
                            object : MyError.ValidationError {
                                override val message: String get() = "DummyValidationError because Invalid Slug"
                                override val key: String get() = "DummyKey"
                            }
                        )
                    ).left(),
                    ResponseEntity(
                        """{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError because Invalid Slug"}]}}""",
                        HttpStatus.valueOf(422)
                    )
                ),
                TestCase(
                    "UseCase:失敗（CommentBody が不正による ValidationError）を返す場合、422 エラーレスポンスを返す",
                    CreateCommentUseCase.Error.InvalidCommentBody(
                        listOf(object : MyError.ValidationError {
                            override val message: String get() = "DummyValidationError because invalid CommentBody"
                            override val key: String get() = "DummyKey"
                        })
                    ).left(),
                    ResponseEntity(
                        """{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError because invalid CommentBody"}]}}""",
                        HttpStatus.valueOf(422)
                    ),
                ),
                TestCase(
                    "UseCase:失敗（Unexpected）を返す場合、500 エラーレスポンスを返す",
                    CreateCommentUseCase.Error.Unexpected(object : MyError {}).left(),
                    ResponseEntity("""{"errors":{"body":["原因不明のエラーが発生しました"]}}""", HttpStatus.valueOf(500)),
                )
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    val actual =
                        commentController(
                            object : MyAuth {
                                override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                                    return dummyRegisteredUser.right()
                                }
                            },
                            object : ListCommentUseCase {},
                            object : CreateCommentUseCase {
                                override fun execute(
                                    slug: String?,
                                    body: String?
                                ): Either<CreateCommentUseCase.Error, Comment> {
                                    return testCase.useCaseExecuteResult
                                }
                            },
                            object : DeleteCommentUseCase {}
                        ).create(requestHeader, pathParam, requestBody)
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }

    @Nested
    class Delete {
        private val requestHeader = "hoge-authorize"
        private val pathParamSlug = "hoge-slug"
        private val pathParamCommentId = "1"
        private val dummyRegisteredUser = RegisteredUser.newWithoutValidation(
            UserId(1),
            Email.newWithoutValidation("dummy@example.com"),
            Username.newWithoutValidation("dummy-name"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
        )
        private fun commentController(
            myAuth: MyAuth,
            listCommentUseCase: ListCommentUseCase,
            createCommentUseCase: CreateCommentUseCase,
            deleteCommentUseCase: DeleteCommentUseCase
        ): CommentController =
            CommentController(myAuth, listCommentUseCase, createCommentUseCase, deleteCommentUseCase)

        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<DeleteCommentUseCase.Error, Unit>,
            val expected: ResponseEntity<String>,
        )

        @TestFactory
        fun deleteTest(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    "UseCase:成功（Unit）を返す場合、200 レスポンスを返す",
                    Unit.right(),
                    ResponseEntity("", HttpStatus.valueOf(200))
                ),
                TestCase(
                    "UseCase:失敗（Slug が不正のためValidationError）を返す場合、422 エラーレスポンスを返す",
                    DeleteCommentUseCase.Error.InvalidSlug(
                        listOf(object : MyError.ValidationError {
                            override val message: String get() = "DummyValidationError because Invalid Slug"
                            override val key: String get() = "DummyKey"
                        })
                    ).left(),
                    ResponseEntity(
                        """{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError because Invalid Slug"}]}}""",
                        HttpStatus.valueOf(422)
                    ),
                ),
                TestCase(
                    "UseCase:失敗（CommentId が不正のため ValidationError）を返す場合、422 エラーレスポンスを返す",
                    DeleteCommentUseCase.Error.InvalidCommentId(
                        listOf(object : MyError.ValidationError {
                            override val message: String get() = "DummyValidationError because Invalid CommentId"
                            override val key: String get() = "DummyKey"
                        })
                    ).left(),
                    ResponseEntity(
                        """{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError because Invalid CommentId"}]}}""",
                        HttpStatus.valueOf(422)
                    ),
                ),
                TestCase(
                    "UseCase:失敗（slugに該当する記事が見つからなかったため NotFound）を返す場合、404 エラーレスポンスを返す",
                    DeleteCommentUseCase.Error.ArticleNotFoundBySlug(
                        object : MyError {},
                        Slug.newWithoutValidation(pathParamSlug)
                    ).left(),
                    ResponseEntity("""{"errors":{"body":["記事が見つかりませんでした"]}}""", HttpStatus.valueOf(404)),
                ),
                TestCase(
                    "UseCase:失敗（commentId に該当する記事が見つからなかったため NotFound）を返す場合、404 エラーレスポンスを返す",
                    DeleteCommentUseCase.Error.CommentsNotFoundByCommentId(
                        object : MyError {},
                        CommentId.newWithoutValidation(pathParamCommentId.toInt())
                    ).left(),
                    ResponseEntity("""{"errors":{"body":["コメントが見つかりませんでした"]}}""", HttpStatus.valueOf(404)),
                ),
                TestCase(
                    "UseCase:失敗（Undefined）を返す場合、500 エラーレスポンスを返す",
                    DeleteCommentUseCase.Error.Unexpected(object : MyError {}).left(),
                    ResponseEntity("""{"errors":{"body":["原因不明のエラーが発生しました"]}}""", HttpStatus.valueOf(500)),
                )
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    val actual =
                        commentController(
                            object : MyAuth {
                                override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                                    return dummyRegisteredUser.right()
                                }
                            },
                            object : ListCommentUseCase {},
                            object : CreateCommentUseCase {},
                            object : DeleteCommentUseCase {
                                override fun execute(
                                    slug: String?,
                                    commentId: Int?
                                ): Either<DeleteCommentUseCase.Error, Unit> {
                                    return testCase.useCaseExecuteResult
                                }
                            },
                        ).delete(requestHeader, pathParamSlug, pathParamCommentId)
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }
}
