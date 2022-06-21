package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.usecase.ListCommentsUseCase
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
        private inline fun commentController(commentsUseCase: ListCommentsUseCase): CommentController =
            CommentController(commentsUseCase)

        @Test
        fun `コメント取得時、UseCase が「Comment」の配列を返す場合、200レスポンスを返す`() {
            val mockComments = listOf(
                Comment.newWithoutValidation(
                    1,
                    "hoge-body-1",
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    "hoge-author-1"
                ),
                Comment.newWithoutValidation(
                    2,
                    "hoge-body-2",
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
                    "hoge-author-2"
                ),
            )
            val listReturnComment = object : ListCommentsUseCase {
                override fun execute(slug: String?): Either<ListCommentsUseCase.Error, kotlin.collections.List<Comment>> =
                    mockComments.right()
            }
            val actual = commentController(listReturnComment).list(pathParam)
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
            val actual = commentController(listReturnNotFoundError).list(pathParam)
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
            val actual = commentController(listReturnValidationError).list(pathParam)
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
            val actual = commentController(listReturnUnexpectedError).list(pathParam)
            val expected = ResponseEntity("""{"errors":{"body":["原因不明のエラーが発生しました"]}}""", HttpStatus.valueOf(500))
            assertThat(actual).isEqualTo(expected)
        }
    }
}
