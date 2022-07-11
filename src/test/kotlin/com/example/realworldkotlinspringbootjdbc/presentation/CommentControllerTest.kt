package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
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
import com.example.realworldkotlinspringbootjdbc.usecase.CreateCommentUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.DeleteCommentUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.ListCommentUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
            CommentController(commentsUseCase, createCommentUseCase, deleteCommentUseCase, myAuth)

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
                                override fun execute(slug: String?): Either<ListCommentUseCase.Error, kotlin.collections.List<Comment>> =
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
        private val notImplementedListCommentUseCase = object : ListCommentUseCase {}
        private val notImplementedDeleteCommentUseCase = object : DeleteCommentUseCase {}
        private fun commentController(
            myAuth: MyAuth,
            listCommentUseCase: ListCommentUseCase,
            createCommentUseCase: CreateCommentUseCase,
            deleteCommentUseCase: DeleteCommentUseCase
        ): CommentController =
            CommentController(listCommentUseCase, createCommentUseCase, deleteCommentUseCase, myAuth)

        private val authorizedMyAuth = object : MyAuth {
            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                return dummyRegisteredUser.right()
            }
        }

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
                            true,
                        )
                    ).right(),
                    ResponseEntity(
                        """{"Comment":{"id":1,"body":"hoge-body","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","author":"hoge-username"}}""",
                        HttpStatus.valueOf(200)
                    )
                ),
                TestCase(
                    "UseCase:失敗（Slug が不正によるValidationError）を返す場合、422 エラーレスポンスを返す",
                    CreateCommentUseCase.Error.InvalidSlug(listOf(
                        object : MyError.ValidationError {
                            override val message: String get() = "DummyValidationError because Invalid Slug"
                            override val key: String get() = "DummyKey"
                        }
                    )).left(),
                    ResponseEntity(
                        """{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError because Invalid Slug"}]}}""",
                        HttpStatus.valueOf(422)
                    )
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
                        ).create(pathParam, pathParam, requestBody)
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }

        @Test
        fun `コメント作成時、CommentBody が不正であることに起因する「バリデーションエラー」のとき、422 エラーレスポンスを返す`() {
            val notImplementedValidationError = object : MyError.ValidationError {
                override val message: String get() = "DummyValidationError because invalid CommentBody"
                override val key: String get() = "DummyKey"
            }
            val createReturnValidationError = object : CreateCommentUseCase {
                override fun execute(slug: String?, body: String?): Either<CreateCommentUseCase.Error, Comment> {
                    return CreateCommentUseCase.Error.InvalidCommentBody(listOf(notImplementedValidationError)).left()
                }
            }
            val actual = commentController(
                authorizedMyAuth,
                notImplementedListCommentUseCase,
                createReturnValidationError,
                notImplementedDeleteCommentUseCase
            ).create(requestHeader, pathParam, requestBody)
            val expected = ResponseEntity(
                """{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError because invalid CommentBody"}]}}""",
                HttpStatus.valueOf(422)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `コメント作成時、Slug に該当する Article が見つからなかったことに起因する「Not Found」エラーのとき、404 エラーレスポンスを返す`() {
            val notImplementedError = object : MyError {}
            val createReturnNotFoundError = object : CreateCommentUseCase {
                override fun execute(slug: String?, body: String?): Either<CreateCommentUseCase.Error, Comment> {
                    return CreateCommentUseCase.Error.NotFound(notImplementedError).left()
                }
            }
            val actual = commentController(
                authorizedMyAuth,
                notImplementedListCommentUseCase,
                createReturnNotFoundError,
                notImplementedDeleteCommentUseCase
            ).create(requestHeader, pathParam, requestBody)
            val expected = ResponseEntity("""{"errors":{"body":["記事が見つかりませんでした"]}}""", HttpStatus.valueOf(404))
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `コメント作成時、UseCase が原因不明のエラーを返す場合、500 エラーレスポンスを返す`() {
            val notImplementedError = object : MyError {}
            val createReturnUnexpectedError = object : CreateCommentUseCase {
                override fun execute(slug: String?, body: String?): Either<CreateCommentUseCase.Error, Comment> {
                    return CreateCommentUseCase.Error.Unexpected(notImplementedError).left()
                }
            }
            val actual = commentController(
                authorizedMyAuth,
                notImplementedListCommentUseCase,
                createReturnUnexpectedError,
                notImplementedDeleteCommentUseCase,
            ).create(requestHeader, pathParam, requestBody)
            val expected = ResponseEntity("""{"errors":{"body":["原因不明のエラーが発生しました"]}}""", HttpStatus.valueOf(500))
            assertThat(actual).isEqualTo(expected)
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
        private val notImplementedListCommentUseCase = object : ListCommentUseCase {}
        private val notImplementedCreateCommentUseCase = object : CreateCommentUseCase {}
        private fun commentController(
            listCommentUseCase: ListCommentUseCase,
            createCommentUseCase: CreateCommentUseCase,
            deleteCommentUseCase: DeleteCommentUseCase,
            myAuth: MyAuth
        ): CommentController =
            CommentController(listCommentUseCase, createCommentUseCase, deleteCommentUseCase, myAuth)

        private val authorizedMyAuth = object : MyAuth {
            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                return dummyRegisteredUser.right()
            }
        }

        @Test
        fun `コメント削除時、UseCase が Unit を返す場合、200 レスポンスを返す`() {
            val deleteCommentUseCase = object : DeleteCommentUseCase {
                override fun execute(slug: String?, commentId: Int?): Either<DeleteCommentUseCase.Error, Unit> {
                    return Unit.right()
                }
            }
            val actual = commentController(
                notImplementedListCommentUseCase,
                notImplementedCreateCommentUseCase,
                deleteCommentUseCase,
                authorizedMyAuth
            ).delete(requestHeader, pathParamSlug, pathParamCommentId)
            val expected = ResponseEntity("", HttpStatus.valueOf(200))
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `コメント削除時、Slug が不正であることに起因する「バリデーションエラー」のとき、422 エラーレスポンスを返す`() {
            val notImplementedValidationError = object : MyError.ValidationError {
                override val message: String get() = "DummyValidationError because Invalid Slug"
                override val key: String get() = "DummyKey"
            }
            val deleteReturnValidationError = object : DeleteCommentUseCase {
                override fun execute(slug: String?, commentId: Int?): Either<DeleteCommentUseCase.Error, Unit> {
                    return DeleteCommentUseCase.Error.InvalidSlug(listOf(notImplementedValidationError)).left()
                }
            }
            val actual = commentController(
                notImplementedListCommentUseCase,
                notImplementedCreateCommentUseCase,
                deleteReturnValidationError,
                authorizedMyAuth
            ).delete(requestHeader, pathParamSlug, pathParamCommentId)
            val expected = ResponseEntity(
                """{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError because Invalid Slug"}]}}""",
                HttpStatus.valueOf(422)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `コメント削除時、CommentId が不正であることに起因する「バリデーションエラー」のとき、422 エラーレスポンスを返す`() {
            val notImplementedValidationError = object : MyError.ValidationError {
                override val message: String get() = "DummyValidationError because Invalid CommentId"
                override val key: String get() = "DummyKey"
            }
            val deleteReturnValidationError = object : DeleteCommentUseCase {
                override fun execute(slug: String?, commentId: Int?): Either<DeleteCommentUseCase.Error, Unit> {
                    return DeleteCommentUseCase.Error.InvalidCommentId(listOf(notImplementedValidationError)).left()
                }
            }
            val actual = commentController(
                notImplementedListCommentUseCase,
                notImplementedCreateCommentUseCase,
                deleteReturnValidationError,
                authorizedMyAuth
            ).delete(requestHeader, pathParamSlug, pathParamCommentId)
            val expected = ResponseEntity(
                """{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError because Invalid CommentId"}]}}""",
                HttpStatus.valueOf(422)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `コメント削除時、UseCase がSlug に該当する記事が見つからなかったことに起因する「NotFound」エラーのとき、404 エラーレスポンスを返す`() {
            val notImplementedError = object : MyError {}
            val deleteReturnArticleNotFoundError = object : DeleteCommentUseCase {
                override fun execute(slug: String?, commentId: Int?): Either<DeleteCommentUseCase.Error, Unit> {
                    return DeleteCommentUseCase.Error.ArticleNotFoundBySlug(
                        notImplementedError,
                        Slug.newWithoutValidation(pathParamSlug)
                    ).left()
                }
            }
            val actual = commentController(
                notImplementedListCommentUseCase,
                notImplementedCreateCommentUseCase,
                deleteReturnArticleNotFoundError,
                authorizedMyAuth
            ).delete(requestHeader, pathParamSlug, pathParamCommentId)
            val expected = ResponseEntity("""{"errors":{"body":["記事が見つかりませんでした"]}}""", HttpStatus.valueOf(404))
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `コメント削除時、UseCase が CommentId に該当するコメントが見つからなかったことに起因する「NotFound」エラーのとき、404 エラーレスポンスを返す`() {
            val notImplementedError = object : MyError {}

            /**
             * FIXME
             *   - DeleteCommentUseCaseの実装をもっと良い名前にする
             *   問題
             *   - GitHub Actionsで〇〇ReturnCommentNotFoundErrorという変数(object)を用意しようとすると、Permission deniedで落ちる
             *   問題に対して行った対応
             *   - 変数名を変える
             */
            val notFoundError = object : DeleteCommentUseCase {
                override fun execute(slug: String?, commentId: Int?): Either<DeleteCommentUseCase.Error, Unit> {
                    return DeleteCommentUseCase.Error.CommentsNotFoundByCommentId(
                        notImplementedError,
                        CommentId.newWithoutValidation(pathParamCommentId.toInt())
                    ).left()
                }
            }
            val actual = commentController(
                notImplementedListCommentUseCase,
                notImplementedCreateCommentUseCase,
                notFoundError,
                authorizedMyAuth
            ).delete(requestHeader, pathParamSlug, pathParamCommentId)
            val expected = ResponseEntity("""{"errors":{"body":["コメントが見つかりませんでした"]}}""", HttpStatus.valueOf(404))
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `コメント削除時、UseCase が原因不明のエラーを返す場合、500 エラーレスポンスを返す`() {
            val notImplementedError = object : MyError {}
            val deleteReturnUnexpectedError = object : DeleteCommentUseCase {
                override fun execute(slug: String?, commentId: Int?): Either<DeleteCommentUseCase.Error, Unit> {
                    return DeleteCommentUseCase.Error.Unexpected(notImplementedError).left()
                }
            }
            val actual = commentController(
                notImplementedListCommentUseCase,
                notImplementedCreateCommentUseCase,
                deleteReturnUnexpectedError,
                authorizedMyAuth,
            ).delete(requestHeader, pathParamSlug, pathParamCommentId)
            val expected = ResponseEntity("""{"errors":{"body":["原因不明のエラーが発生しました"]}}""", HttpStatus.valueOf(500))
            assertThat(actual).isEqualTo(expected)
        }
    }
}
