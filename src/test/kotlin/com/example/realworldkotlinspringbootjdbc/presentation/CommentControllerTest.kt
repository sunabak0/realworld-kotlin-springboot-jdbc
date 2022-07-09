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
import com.example.realworldkotlinspringbootjdbc.usecase.CreateCommentUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.DeleteCommentUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.ListCommentUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.text.SimpleDateFormat
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body as CommentBody

class CommentControllerTest {
    @Nested
    class `List(コメント取得) JWT 認証成功` {
        private val requestHeader = "hoge-authorize"
        private val pathParam = "hoge-slug"
        val dummyRegisteredUser = RegisteredUser.newWithoutValidation(
            UserId(1),
            Email.newWithoutValidation("dummy@example.com"),
            Username.newWithoutValidation("dummy-name"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
        )
        private fun commentController(
            myAuth: MyAuth,
            commentsUseCase: ListCommentUseCase,
            createCommentUseCase: CreateCommentUseCase,
            deleteCommentUseCase: DeleteCommentUseCase
        ): CommentController =
            CommentController(myAuth, commentsUseCase, createCommentUseCase, deleteCommentUseCase)

        private val authorizedMyAuth = object : MyAuth {
            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                return dummyRegisteredUser.right()
            }
        }

        private val notImplementedCreateCommentUseCase = object : CreateCommentUseCase {}

        private val notImplementedDeleteCommentUseCase = object : DeleteCommentUseCase {}

        @Test
        fun `JWT 認証成功-コメント取得-UseCase が「Comment」のリストを返す場合、200レスポンスを返す`() {
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
            )
            val listReturnComment = object : ListCommentUseCase {
                override fun execute(
                    slug: String?,
                    currentUser: Option<RegisteredUser>
                ): Either<ListCommentUseCase.Error, kotlin.collections.List<Comment>> =
                    mockComments.right()
            }
            val actual =
                commentController(
                    authorizedMyAuth,
                    listReturnComment,
                    notImplementedCreateCommentUseCase,
                    notImplementedDeleteCommentUseCase
                ).list(
                    requestHeader,
                    pathParam
                )
            val expected = ResponseEntity(
                """{"comments":[{"id":1,"body":"hoge-body-1","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","author":"hoge-author-1"},{"id":2,"body":"hoge-body-2","createdAt":"2022-02-01T15:00:00.000Z","updatedAt":"2022-02-01T15:00:00.000Z","author":"hoge-author-2"}]}""",
                HttpStatus.valueOf(200)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `JWT 認証成功-コメント取得-UseCase が「NotFound」を返す場合、404 エラーレスポンスを返す`() {
            val notImplementedError = object : MyError {}
            val listReturnNotFoundError = object : ListCommentUseCase {
                override fun execute(
                    slug: String?,
                    currentUser: Option<RegisteredUser>
                ): Either<ListCommentUseCase.Error, kotlin.collections.List<Comment>> =
                    ListCommentUseCase.Error.NotFound(notImplementedError).left()
            }
            val actual = commentController(
                authorizedMyAuth,
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
        fun `JWT 認証成功-コメント取得-UseCase が「バリデーションエラー」を返す場合、422 エラーレスポンスを返す`() {
            val notImplementedValidationError = object : MyError.ValidationError {
                override val message: String get() = "DummyValidationError"
                override val key: String get() = "DummyKey"
            }
            val listReturnValidationError = object : ListCommentUseCase {
                override fun execute(
                    slug: String?,
                    currentUser: Option<RegisteredUser>
                ): Either<ListCommentUseCase.Error, kotlin.collections.List<Comment>> {
                    return ListCommentUseCase.Error.InvalidSlug(listOf(notImplementedValidationError)).left()
                }
            }
            val actual = commentController(
                authorizedMyAuth,
                listReturnValidationError,
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
        fun `JWT 認証成功-コメント取得-UseCase が原因不明のエラーを返す場合、500 エラーレスポンスを返す`() {
            val notImplementedError = object : MyError {}
            val listReturnUnexpectedError = object : ListCommentUseCase {
                override fun execute(
                    slug: String?,
                    currentUser: Option<RegisteredUser>
                ): Either<ListCommentUseCase.Error, kotlin.collections.List<Comment>> {
                    return ListCommentUseCase.Error.Unexpected(notImplementedError).left()
                }
            }
            val actual = commentController(
                authorizedMyAuth,
                listReturnUnexpectedError,
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
            )
            val listReturnComment = object : ListCommentUseCase {
                override fun execute(
                    slug: String?,
                    currentUser: Option<RegisteredUser>
                ): Either<ListCommentUseCase.Error, kotlin.collections.List<Comment>> =
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
                """{"comments":[{"id":1,"body":"hoge-body-1","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","author":"hoge-author-1"},{"id":2,"body":"hoge-body-2","createdAt":"2022-02-01T15:00:00.000Z","updatedAt":"2022-02-01T15:00:00.000Z","author":"hoge-author-2"}]}""",
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
                ): Either<ListCommentUseCase.Error, kotlin.collections.List<Comment>> =
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

        // @Test
        // fun `JWT 認証失敗 or 未ログイン-コメント取得-UseCase が「バリデーションエラー」を返す場合、404 エラーレスポンスを返す`() {
        //     val notImplementedValidationError = object : MyError.ValidationError {
        //         override val message: String get() = "DummyValidationError"
        //         override val key: String get() = "DummyKey"
        //     }
        //     val listReturnValidationError = object : ListCommentUseCase {
        //         override fun execute(
        //             slug: String?,
        //             currentUser: Option<RegisteredUser>
        //         ): Either<ListCommentUseCase.Error, List<Comment>> {
        //             return ListCommentUseCase.Error.InvalidSlug(listOf(notImplementedValidationError)).left()
        //         }
        //     }
        //     val actual = commentController(
        //         unauthorizedMyAuth,
        //         listReturnValidationError,
        //         notImplementedCreateCommentUseCase,
        //         notImplementedDeleteCommentUseCase
        //     ).list(
        //         requestHeader,
        //         pathParam
        //     )
        //     val expected = ResponseEntity(
        //         """{"errors":{"body":["記事が見つかりませんでした"]}}""",
        //         HttpStatus.valueOf(404)
        //     )
        //     assertThat(actual).isEqualTo(expected)
        // }

        @Test
        fun `JWT 認証失敗 or 未ログイン-コメント取得-UseCase が原因不明のエラーを返す場合、500 エラーレスポンスを返す`() {
            val notImplementedError = object : MyError {}
            val listReturnUnexpectedError = object : ListCommentUseCase {
                override fun execute(
                    slug: String?,
                    currentUser: Option<RegisteredUser>
                ): Either<ListCommentUseCase.Error, kotlin.collections.List<Comment>> {
                    return ListCommentUseCase.Error.Unexpected(notImplementedError).left()
                }
            }
            val actual = commentController(
                unauthorizedMyAuth,
                listReturnUnexpectedError,
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
        private val notImplementedListCommentUseCase = object : ListCommentUseCase {}
        private val notImplementedDeleteCommentUseCase = object : DeleteCommentUseCase {}
        private fun commentController(
            myAuth: MyAuth,
            listCommentUseCase: ListCommentUseCase,
            createCommentUseCase: CreateCommentUseCase,
            deleteCommentUseCase: DeleteCommentUseCase
        ): CommentController =
            CommentController(myAuth, listCommentUseCase, createCommentUseCase, deleteCommentUseCase)

        private val authorizedMyAuth = object : MyAuth {
            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                return dummyRegisteredUser.right()
            }
        }

        @Test
        fun `コメント作成時、UseCase がコメント作成したコメントを返す場合、200 レスポンスを返す`() {
            val returnComment = Comment.newWithoutValidation(
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
            )
            val createCommentUseCase = object : CreateCommentUseCase {
                override fun execute(slug: String?, body: String?): Either<CreateCommentUseCase.Error, Comment> {
                    return returnComment.right()
                }
            }
            val actual =
                commentController(
                    authorizedMyAuth,
                    notImplementedListCommentUseCase,
                    createCommentUseCase,
                    notImplementedDeleteCommentUseCase
                ).create(pathParam, pathParam, requestBody)
            val expected = ResponseEntity(
                """{"Comment":{"id":1,"body":"hoge-body","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","author":"hoge-username"}}""",
                HttpStatus.valueOf(200)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `コメント作成時、Slug が不正であることに起因する「バリデーションエラー」のとき、422 エラーレスポンスを返す`() {
            val notImplementedValidationError = object : MyError.ValidationError {
                override val message: String get() = "DummyValidationError because Invalid Slug"
                override val key: String get() = "DummyKey"
            }
            val createReturnValidationError = object : CreateCommentUseCase {
                override fun execute(slug: String?, body: String?): Either<CreateCommentUseCase.Error, Comment> {
                    return CreateCommentUseCase.Error.InvalidSlug(listOf(notImplementedValidationError)).left()
                }
            }
            val actual = commentController(
                authorizedMyAuth,
                notImplementedListCommentUseCase,
                createReturnValidationError,
                notImplementedDeleteCommentUseCase
            ).create(requestHeader, pathParam, requestBody)
            val expected = ResponseEntity(
                """{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError because Invalid Slug"}]}}""",
                HttpStatus.valueOf(422)
            )
            assertThat(actual).isEqualTo(expected)
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
            myAuth: MyAuth,
            listCommentUseCase: ListCommentUseCase,
            createCommentUseCase: CreateCommentUseCase,
            deleteCommentUseCase: DeleteCommentUseCase
        ): CommentController =
            CommentController(myAuth, listCommentUseCase, createCommentUseCase, deleteCommentUseCase)

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
                authorizedMyAuth,
                notImplementedListCommentUseCase,
                notImplementedCreateCommentUseCase,
                deleteCommentUseCase
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
                authorizedMyAuth,
                notImplementedListCommentUseCase,
                notImplementedCreateCommentUseCase,
                deleteReturnValidationError
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
                authorizedMyAuth,
                notImplementedListCommentUseCase,
                notImplementedCreateCommentUseCase,
                deleteReturnValidationError
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
                authorizedMyAuth,
                notImplementedListCommentUseCase,
                notImplementedCreateCommentUseCase,
                deleteReturnArticleNotFoundError
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
                authorizedMyAuth,
                notImplementedListCommentUseCase,
                notImplementedCreateCommentUseCase,
                notFoundError
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
                authorizedMyAuth,
                notImplementedListCommentUseCase,
                notImplementedCreateCommentUseCase,
                deleteReturnUnexpectedError,
            ).delete(requestHeader, pathParamSlug, pathParamCommentId)
            val expected = ResponseEntity("""{"errors":{"body":["原因不明のエラーが発生しました"]}}""", HttpStatus.valueOf(500))
            assertThat(actual).isEqualTo(expected)
        }
    }
}
