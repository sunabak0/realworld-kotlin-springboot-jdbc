package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.Profile
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.usecase.CreateCommentsUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.DeleteCommentsUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.ListCommentsUseCase
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
    class List {
        private val pathParam = "hoge-slug"
        private inline fun commentController(
            commentsUseCase: ListCommentsUseCase,
            createCommentsUseCase: CreateCommentsUseCase,
            deleteCommentsUseCase: DeleteCommentsUseCase,
            myAuth: MyAuth
        ): CommentController =
            CommentController(commentsUseCase, createCommentsUseCase, deleteCommentsUseCase, myAuth)

        private val notImplementedMyAuth = object : MyAuth {}

        private val notImplementedCreateCommentsUseCase = object : CreateCommentsUseCase {}

        private val notImplementedDeleteCommentsUseCase = object : DeleteCommentsUseCase {}

        @Test
        fun `コメント取得時、UseCase が「Comment」のリストを返す場合、200レスポンスを返す`() {
            val mockComments = listOf(
                Comment.newWithoutValidation(
                    CommentId(1),
                    CommentBody.newWithoutValidation("hoge-body-1"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    Profile.newWithoutValidation(
                        Username.newWithoutValidation("hoge-author-1"),
                        Bio.newWithoutValidation("hoge-bio-1"),
                        Image.newWithoutValidation("hoge-image-1"),
                        false
                    )
                ),
                Comment.newWithoutValidation(
                    CommentId(2),
                    CommentBody.newWithoutValidation("hoge-body-2"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
                    Profile.newWithoutValidation(
                        Username.newWithoutValidation("hoge-author-2"),
                        Bio.newWithoutValidation("hoge-bio-2"),
                        Image.newWithoutValidation("hoge-image-2"),
                        false
                    )
                ),
            )
            val listReturnComment = object : ListCommentsUseCase {
                override fun execute(slug: String?): Either<ListCommentsUseCase.Error, kotlin.collections.List<Comment>> =
                    mockComments.right()
            }
            val actual =
                commentController(
                    listReturnComment,
                    notImplementedCreateCommentsUseCase,
                    notImplementedDeleteCommentsUseCase,
                    notImplementedMyAuth
                ).list(
                    pathParam
                )
            val expected = ResponseEntity(
                """{"comments":[{"id":1,"body":"hoge-body-1","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","author":"hoge-author-1"},{"id":2,"body":"hoge-body-2","createdAt":"2022-02-01T15:00:00.000Z","updatedAt":"2022-02-01T15:00:00.000Z","author":"hoge-author-2"}]}""",
                HttpStatus.valueOf(200)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `コメント取得時、UseCase が「NotFound」を返す場合、404 エラーレスポンスを返す`() {
            val notImplementedError = object : MyError {}
            val listReturnNotFoundError = object : ListCommentsUseCase {
                override fun execute(slug: String?): Either<ListCommentsUseCase.Error, kotlin.collections.List<Comment>> =
                    ListCommentsUseCase.Error.NotFound(notImplementedError).left()
            }
            val actual = commentController(
                listReturnNotFoundError,
                notImplementedCreateCommentsUseCase,
                notImplementedDeleteCommentsUseCase,
                notImplementedMyAuth
            ).list(pathParam)
            val expected = ResponseEntity("""{"errors":{"body":["記事が見つかりませんでした"]}}""", HttpStatus.valueOf(404))
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `コメント取得時、UseCase が「バリデーションエラー」を返す場合、422 エラーレスポンスを返す`() {
            val notImplementedValidationError = object : MyError.ValidationError {
                override val message: String get() = "DummyValidationError"
                override val key: String get() = "DummyKey"
            }
            val listReturnValidationError = object : ListCommentsUseCase {
                override fun execute(slug: String?): Either<ListCommentsUseCase.Error, kotlin.collections.List<Comment>> {
                    return ListCommentsUseCase.Error.InvalidSlug(listOf(notImplementedValidationError)).left()
                }
            }
            val actual = commentController(
                listReturnValidationError,
                notImplementedCreateCommentsUseCase,
                notImplementedDeleteCommentsUseCase,
                notImplementedMyAuth
            ).list(pathParam)
            val expected = ResponseEntity(
                """{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError"}]}}""",
                HttpStatus.valueOf(422)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `コメント取得時、UseCase が原因不明のエラーを返す場合、500 エラーレスポンスを返す`() {
            val notImplementedError = object : MyError {}
            val listReturnUnexpectedError = object : ListCommentsUseCase {
                override fun execute(slug: String?): Either<ListCommentsUseCase.Error, kotlin.collections.List<Comment>> {
                    return ListCommentsUseCase.Error.Unexpected(notImplementedError).left()
                }
            }
            val actual = commentController(
                listReturnUnexpectedError,
                notImplementedCreateCommentsUseCase,
                notImplementedDeleteCommentsUseCase,
                notImplementedMyAuth
            ).list(pathParam)
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
            1,
            "dummy@example.com",
            "dummy-name",
            "dummy-bio",
            "dummy-image",
        )
        private val notImplementedListCommentsUseCase = object : ListCommentsUseCase {}
        private val notImplementedDeleteCommentsUseCase = object : DeleteCommentsUseCase {}
        private inline fun commentController(
            listCommentsUseCase: ListCommentsUseCase,
            createCommentsUseCase: CreateCommentsUseCase,
            deleteCommentsUseCase: DeleteCommentsUseCase,
            myAuth: MyAuth
        ): CommentController =
            CommentController(listCommentsUseCase, createCommentsUseCase, deleteCommentsUseCase, myAuth)

        private val authorizedMyAuth = object : MyAuth {
            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                return dummyRegisteredUser.right()
            }
        }

        @Test
        fun `コメント作成時、UseCase がコメント作成したコメントを返す場合、200 レスポンスを返す`() {
            val returnComment = Comment.newWithoutValidation(
                CommentId(1),
                CommentBody.newWithoutValidation("hoge-body"),
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                Profile.newWithoutValidation(
                    Username.newWithoutValidation("hoge-username"),
                    Bio.newWithoutValidation(""),
                    Image.newWithoutValidation(""),
                    true
                )
            )
            val createCommentsUseCase = object : CreateCommentsUseCase {
                override fun execute(slug: String?, body: String?): Either<CreateCommentsUseCase.Error, Comment> {
                    return returnComment.right()
                }
            }
            val actual =
                commentController(
                    notImplementedListCommentsUseCase,
                    createCommentsUseCase,
                    notImplementedDeleteCommentsUseCase,
                    authorizedMyAuth
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
            val createReturnValidationError = object : CreateCommentsUseCase {
                override fun execute(slug: String?, body: String?): Either<CreateCommentsUseCase.Error, Comment> {
                    return CreateCommentsUseCase.Error.InvalidSlug(listOf(notImplementedValidationError)).left()
                }
            }
            val actual = commentController(
                notImplementedListCommentsUseCase,
                createReturnValidationError,
                notImplementedDeleteCommentsUseCase,
                authorizedMyAuth
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
            val createReturnValidationError = object : CreateCommentsUseCase {
                override fun execute(slug: String?, body: String?): Either<CreateCommentsUseCase.Error, Comment> {
                    return CreateCommentsUseCase.Error.InvalidCommentBody(listOf(notImplementedValidationError)).left()
                }
            }
            val actual = commentController(
                notImplementedListCommentsUseCase,
                createReturnValidationError,
                notImplementedDeleteCommentsUseCase,
                authorizedMyAuth
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
            val createReturnNotFoundError = object : CreateCommentsUseCase {
                override fun execute(slug: String?, body: String?): Either<CreateCommentsUseCase.Error, Comment> {
                    return CreateCommentsUseCase.Error.NotFound(notImplementedError).left()
                }
            }
            val actual = commentController(
                notImplementedListCommentsUseCase,
                createReturnNotFoundError,
                notImplementedDeleteCommentsUseCase,
                authorizedMyAuth
            ).create(requestHeader, pathParam, requestBody)
            val expected = ResponseEntity("""{"errors":{"body":["記事が見つかりませんでした"]}}""", HttpStatus.valueOf(404))
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `コメント作成時、UseCase が原因不明のエラーを返す場合、500 エラーレスポンスを返す`() {
            val notImplementedError = object : MyError {}
            val createReturnUnexpectedError = object : CreateCommentsUseCase {
                override fun execute(slug: String?, body: String?): Either<CreateCommentsUseCase.Error, Comment> {
                    return CreateCommentsUseCase.Error.Unexpected(notImplementedError).left()
                }
            }
            val actual = commentController(
                notImplementedListCommentsUseCase,
                createReturnUnexpectedError,
                notImplementedDeleteCommentsUseCase,
                authorizedMyAuth,
            ).create(requestHeader, pathParam, requestBody)
            val expected = ResponseEntity("""{"errors":{"body":["原因不明のエラーが発生しました"]}}""", HttpStatus.valueOf(500))
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    class Delete {
        private val requestHeader = "hoge-authorize"
        private val pathParamSlug = "hoge-slug"
        private val pathParamCommentId = 1
        private val dummyRegisteredUser = RegisteredUser.newWithoutValidation(
            1,
            "dummy@example.com",
            "dummy-name",
            "dummy-bio",
            "dummy-image",
        )
        private val notImplementedListCommentsUseCase = object : ListCommentsUseCase {}
        private val notImplementedCreateCommentsUseCase = object : CreateCommentsUseCase {}
        private inline fun commentController(
            listCommentsUseCase: ListCommentsUseCase,
            createCommentsUseCase: CreateCommentsUseCase,
            deleteCommentsUseCase: DeleteCommentsUseCase,
            myAuth: MyAuth
        ): CommentController =
            CommentController(listCommentsUseCase, createCommentsUseCase, deleteCommentsUseCase, myAuth)

        private val authorizedMyAuth = object : MyAuth {
            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                return dummyRegisteredUser.right()
            }
        }

        @Test
        fun `コメント削除時、UseCase が Unit を返す場合、200 レスポンスを返す`() {
            val deleteCommentsUseCase = object : DeleteCommentsUseCase {
                override fun execute(slug: String?, commentId: Int?): Either<DeleteCommentsUseCase.Error, Unit> {
                    return Unit.right()
                }
            }
            val actual = commentController(
                notImplementedListCommentsUseCase,
                notImplementedCreateCommentsUseCase,
                deleteCommentsUseCase,
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
            val deleteReturnValidationError = object : DeleteCommentsUseCase {
                override fun execute(slug: String?, commentId: Int?): Either<DeleteCommentsUseCase.Error, Unit> {
                    return DeleteCommentsUseCase.Error.InvalidSlug(listOf(notImplementedValidationError)).left()
                }
            }
            val actual = commentController(
                notImplementedListCommentsUseCase,
                notImplementedCreateCommentsUseCase,
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
            val deleteReturnValidationError = object : DeleteCommentsUseCase {
                override fun execute(slug: String?, commentId: Int?): Either<DeleteCommentsUseCase.Error, Unit> {
                    return DeleteCommentsUseCase.Error.InvalidCommentId(listOf(notImplementedValidationError)).left()
                }
            }
            val actual = commentController(
                notImplementedListCommentsUseCase,
                notImplementedCreateCommentsUseCase,
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
            val deleteReturnArticleNotFoundError = object : DeleteCommentsUseCase {
                override fun execute(slug: String?, commentId: Int?): Either<DeleteCommentsUseCase.Error, Unit> {
                    return DeleteCommentsUseCase.Error.ArticleNotFoundBySlug(
                        notImplementedError,
                        Slug.newWithoutValidation(pathParamSlug)
                    ).left()
                }
            }
            val actual = commentController(
                notImplementedListCommentsUseCase,
                notImplementedCreateCommentsUseCase,
                deleteReturnArticleNotFoundError,
                authorizedMyAuth
            ).delete(requestHeader, pathParamSlug, pathParamCommentId)
            val expected = ResponseEntity("""{"errors":{"body":["記事が見つかりませんでした"]}}""", HttpStatus.valueOf(404))
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `コメント削除時、UseCase が CommentId に該当するコメントが見つからなかったことに起因する「NotFound」エラーのとき、404 エラーレスポンスを返す`() {
            val notImplementedError = object : MyError {}
            val deleteReturnCommentNotFoundError = object : DeleteCommentsUseCase {
                override fun execute(slug: String?, commentId: Int?): Either<DeleteCommentsUseCase.Error, Unit> {
                    return DeleteCommentsUseCase.Error.CommentsNotFoundByCommentId(
                        notImplementedError,
                        CommentId(pathParamCommentId)
                    ).left()
                }
            }
            val actual = commentController(
                notImplementedListCommentsUseCase,
                notImplementedCreateCommentsUseCase,
                deleteReturnCommentNotFoundError,
                authorizedMyAuth
            ).delete(requestHeader, pathParamSlug, pathParamCommentId)
            val expected = ResponseEntity("""{"errors":{"body":["コメントが見つかりませんでした"]}}""", HttpStatus.valueOf(404))
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `コメント削除時、UseCase が原因不明のエラーを返す場合、500 エラーレスポンスを返す`() {
            val notImplementedError = object : MyError {}
            val deleteReturnUnexpectedError = object : DeleteCommentsUseCase {
                override fun execute(slug: String?, commentId: Int?): Either<DeleteCommentsUseCase.Error, Unit> {
                    return DeleteCommentsUseCase.Error.Unexpected(notImplementedError).left()
                }
            }
            val actual = commentController(
                notImplementedListCommentsUseCase,
                notImplementedCreateCommentsUseCase,
                deleteReturnUnexpectedError,
                authorizedMyAuth,
            ).delete(requestHeader, pathParamSlug, pathParamCommentId)
            val expected = ResponseEntity("""{"errors":{"body":["原因不明のエラーが発生しました"]}}""", HttpStatus.valueOf(500))
            assertThat(actual).isEqualTo(expected)
        }
    }
}
