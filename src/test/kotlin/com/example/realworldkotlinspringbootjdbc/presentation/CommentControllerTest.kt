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
            val dummyComments = listOf<Comment>(
                Comment.newWithoutValidation(
                    id = 1,
                    body = "hoge-body-1",
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    author = "hoge-author-1"
                ),
                Comment.newWithoutValidation(
                    id = 2,
                    body = "hoge-body-2",
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
                    author = "hoge-author-2"
                ),
            )
            val returnedComments = object : ListCommentsUseCase {
                override fun execute(slug: String?): Either<ListCommentsUseCase.Error, kotlin.collections.List<Comment>> =
                    dummyComments.right()
            }
            val actual = commentController(returnedComments).list(pathParam)
            val expected = ResponseEntity(
                """{"comments":[{"id":1,"body":"hoge-body-1","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","author":"hoge-author-1"},{"id":2,"body":"hoge-body-2","createdAt":"2022-02-01T15:00:00.000Z","updatedAt":"2022-02-01T15:00:00.000Z","author":"hoge-author-2"}]}""",
                HttpStatus.valueOf(200)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `コメント取得時、UseCase が「NotFound」を返す場合、404エラーレスポンスを返す`() {
            val dummyError = object : MyError {}
            val returnedNotFoundError = object : ListCommentsUseCase {
                override fun execute(slug: String?): Either<ListCommentsUseCase.Error, kotlin.collections.List<Comment>> =
                    ListCommentsUseCase.Error.NotFound(dummyError).left()
            }
            val actual = commentController(returnedNotFoundError).list(pathParam)
            val expected = ResponseEntity("""{"errors":{"body":["コメントが見つかりませんでした"]}}""", HttpStatus.valueOf(404))
            assertThat(actual).isEqualTo(expected)
        }
    }
}
