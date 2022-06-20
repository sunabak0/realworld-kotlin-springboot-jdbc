package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.presentation.response.Comment
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeMyErrorListForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
import com.example.realworldkotlinspringbootjdbc.usecase.ListCommentsUseCase
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.text.SimpleDateFormat

@RestController
@Tag(name = "Comments")
class CommentController(val listComments: ListCommentsUseCase) {
    @GetMapping("/articles/{slug}/comments")
    fun list(@PathVariable("slug") slug: String?): ResponseEntity<String> {
        return when (val result = listComments.execute(slug)) {
            /**
             * コメント取得に成功
             */
            is Right -> {
                val comments = result.value
                println(comments)
                val comment1 = Comment(
                    1,
                    "hoge-body-1",
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    "hoge-author-1"
                )
                val comment2 = Comment(
                    2,
                    "hoge-body-2",
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
                    "hoge-author-2"
                )
                return ResponseEntity(
                    ObjectMapper().writeValueAsString(
                        mapOf(
                            "comments" to listOf(
                                comment1,
                                comment2,
                            ),
                        )
                    ),
                    HttpStatus.valueOf(200)
                )
            }
            /**
             * コメント取得に失敗
             */
            is Left -> when (val useCaseError = result.value) {
                /**
                 * 原因: バリデーションエラー
                 */
                is ListCommentsUseCase.Error.InvalidSlug -> ResponseEntity(
                    serializeMyErrorListForResponseBody(useCaseError.errors),
                    HttpStatus.valueOf(422)
                )
                /**
                 * 原因: Slug に該当する記事が見つからなかった
                 */
                is ListCommentsUseCase.Error.NotFound -> ResponseEntity(
                    serializeUnexpectedErrorForResponseBody("コメントが見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                    HttpStatus.valueOf(404)
                )
                is ListCommentsUseCase.Error.FailedShow -> TODO()
                is ListCommentsUseCase.Error.Unexpected -> TODO()
            }
        }
    }

    @PostMapping("/articles/{slug}/comments")
    fun create(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        return ResponseEntity(
            ObjectMapper().writeValueAsString(
                mapOf(
                    "comment" to mapOf(
                        "id" to 1,
                        "body" to "hoge-body",
                        "createdAt" to "2022-01-01T00:00:00.0+09:00",
                        "updatedAt" to "2022-01-01T00:00:00.0+09:00",
                        "author" to "hoge-author",
                    ),
                )
            ),
            HttpStatus.valueOf(200)
        )
    }

    @DeleteMapping("/articles/{slug}/comments/{id}")
    fun delete(): ResponseEntity<String> {
        return ResponseEntity("", HttpStatus.valueOf(200))
    }
}
