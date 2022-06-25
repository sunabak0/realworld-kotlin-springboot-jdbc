package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.Profile
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body as CommentBody
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.usecase.CreateCommentsUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.ListCommentsUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.text.SimpleDateFormat

class CommentControllerTest {
    @Nested
    class List {
        private val pathParam = "hoge-slug"
        private inline fun commentController(
            commentsUseCase: ListCommentsUseCase,
            createCommentsUseCase: CreateCommentsUseCase,
            myAuth: MyAuth
        ): CommentController =
            CommentController(commentsUseCase, createCommentsUseCase, myAuth)

        private val notImplementedMyAuth = object : MyAuth {}

        private val notImplementedCreateCommentsUseCase = object : CreateCommentsUseCase {}

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
                commentController(listReturnComment, notImplementedCreateCommentsUseCase, notImplementedMyAuth).list(
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
                notImplementedMyAuth
            ).list(pathParam)
            val expected = ResponseEntity("""{"errors":{"body":["コメントが見つかりませんでした"]}}""", HttpStatus.valueOf(404))
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
                notImplementedMyAuth
            ).list(pathParam)
            val expected = ResponseEntity("""{"errors":{"body":["原因不明のエラーが発生しました"]}}""", HttpStatus.valueOf(500))
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    class Create {
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
        private inline fun commentController(
            listCommentsUseCase: ListCommentsUseCase,
            createCommentsUseCase: CreateCommentsUseCase,
            myAuth: MyAuth
        ): CommentController =
            CommentController(listCommentsUseCase, createCommentsUseCase, myAuth)

        @Test
        fun `コメント作成時、UseCase がコメント作成したコメントを返す場合、201 レスポンスを返す`() {
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
            val notImplementedMyAuth = object : MyAuth {
                override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                    return dummyRegisteredUser.right()
                }
            }
            val createCommentsUseCase = object : CreateCommentsUseCase {
                override fun execute(slug: String?, body: String?): Either<CreateCommentsUseCase.Error, Comment> {
                    return returnComment.right()
                }
            }
            val actual =
                commentController(
                    notImplementedListCommentsUseCase,
                    createCommentsUseCase,
                    notImplementedMyAuth
                ).create(pathParam, requestBody)
            val expected = ResponseEntity(
                """{"comment":{"id":1,"body":"hoge-body","createdAt":"2022-01-01T00:00:00.0+09:00","updatedAt":"2022-01-01T00:00:00.0+09:00","author":"hoge-author"}}""",
                HttpStatus.valueOf(201)
            )
            assertThat(actual).isEqualTo(expected)
        }
    }
}
