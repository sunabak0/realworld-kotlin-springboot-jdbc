package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.presentation.request.NullableComment
import com.example.realworldkotlinspringbootjdbc.presentation.response.Comment
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeMyErrorListForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.shared.AuthorizationError
import com.example.realworldkotlinspringbootjdbc.usecase.CreateCommentsUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.ListCommentsUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Comments")
class CommentController(
    val listCommentsUseCase: ListCommentsUseCase,
    val createCommentsUseCase: CreateCommentsUseCase,
    val myAuth: MyAuth
) {
    @GetMapping("/articles/{slug}/comments")
    fun list(@PathVariable("slug") slug: String?): ResponseEntity<String> {
        return when (val result = listCommentsUseCase.execute(slug)) {
            /**
             * コメント取得に成功
             */
            is Right -> {
                val comments =
                    result.value.map {
                        Comment(
                            it.id.value,
                            it.body.value,
                            it.createdAt,
                            it.updatedAt,
                            it.author.username.value,
                        )
                    }

                return ResponseEntity(
                    ObjectMapper().writeValueAsString(
                        mapOf(
                            "comments" to comments,
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
                 * 原因: 記事が見つかりませんでした
                 */
                is ListCommentsUseCase.Error.NotFound -> ResponseEntity(
                    serializeUnexpectedErrorForResponseBody("コメントが見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                    HttpStatus.valueOf(404)
                )
                /**
                 * 原因: 不明
                 */
                is ListCommentsUseCase.Error.Unexpected -> ResponseEntity(
                    serializeUnexpectedErrorForResponseBody("原因不明のエラーが発生しました"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                    HttpStatus.valueOf(500)
                )
            }
        }
    }

    @PostMapping("/articles/{slug}/comments")
    fun create(
        @RequestHeader("Authorization") rawAuthorizationHeader: String?,
        @PathVariable("slug") slug: String?,
        @RequestBody rawRequestBody: String?
    ): ResponseEntity<String> {
        return when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
            /**
             * JWT 認証 失敗
             */
            is Left -> AuthorizationError.handle(authorizeResult.value)
            /**
             * JWT 認証 成功
             */
            is Right -> {
                val comment = NullableComment.from(rawRequestBody)
                when (val createdComment = createCommentsUseCase.execute(slug, comment.body)) {
                    is Left -> TODO()
                    /**
                     * コメントの登録に成功
                     */
                    is Right -> ResponseEntity(
                        Comment.from(createdComment.value).serializeWithRootName(),
                        HttpStatus.valueOf(201)
                    )
                }
            }
        }
    }

    @DeleteMapping("/articles/{slug}/comments/{id}")
    fun delete(): ResponseEntity<String> {
        return ResponseEntity("", HttpStatus.valueOf(200))
    }
}
