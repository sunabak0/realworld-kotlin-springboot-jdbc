package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Some
import com.example.realworldkotlinspringbootjdbc.presentation.request.NullableComment
import com.example.realworldkotlinspringbootjdbc.presentation.request.NullableCommentId
import com.example.realworldkotlinspringbootjdbc.presentation.response.Comment
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeMyErrorListForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.shared.AuthorizationError
import com.example.realworldkotlinspringbootjdbc.usecase.comment.CreateCommentUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.comment.DeleteCommentUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.comment.ListCommentUseCase
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
    val myAuth: MyAuth,
    val listCommentUseCase: ListCommentUseCase,
    val createCommentUseCase: CreateCommentUseCase,
    val deleteCommentUseCase: DeleteCommentUseCase
) {
    @GetMapping("/articles/{slug}/comments")
    fun list(
        @RequestHeader("Authorization") rawAuthorizationHeader: String?,
        @PathVariable("slug") slug: String?
    ): ResponseEntity<String> {
        return when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
            /**
             * JWT 認証 失敗 or 未ログイン
             */
            is Left -> when (val result = listCommentUseCase.execute(slug)) {
                /**
                 * コメント取得に失敗
                 */
                is Left -> when (result.value) {
                    /**
                     * 原因: バリデーションエラー
                     */
                    is ListCommentUseCase.Error.InvalidSlug -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                    /**
                     * 原因: 記事が見つからなかった
                     */
                    is ListCommentUseCase.Error.NotFound -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                    /**
                     * 原因: 不明
                     */
                    is ListCommentUseCase.Error.Unexpected -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("原因不明のエラーが発生しました"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(500)
                    )
                }
                /**
                 * コメント取得に成功
                 * TODO authorId ではなく、author を戻すように修正する
                 */
                is Right -> {
                    val comments = result.value.map {
                        Comment(
                            it.id.value,
                            it.body.value,
                            it.createdAt,
                            it.updatedAt,
                            it.authorId.value
                        )
                    }
                    ResponseEntity(
                        ObjectMapper().writeValueAsString(
                            mapOf(
                                "comments" to comments,
                            )
                        ),
                        HttpStatus.valueOf(200)
                    )
                }
            }
            /**
             * JWT 認証 成功
             */
            is Right -> when (val result = listCommentUseCase.execute(slug, Some(authorizeResult.value))) {
                /**
                 * コメント取得に成功
                 * TODO authorId ではなく、author を戻すように修正する
                 */
                is Right -> {
                    val comments =
                        result.value.map {
                            Comment(
                                it.id.value,
                                it.body.value,
                                it.createdAt,
                                it.updatedAt,
                                it.authorId.value,
                            )
                        }

                    ResponseEntity(
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
                is Left -> when (result.value) {
                    /**
                     * 原因: バリデーションエラー
                     */
                    is ListCommentUseCase.Error.InvalidSlug -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                    /**
                     * 原因: 記事が見つかりませんでした
                     */
                    is ListCommentUseCase.Error.NotFound -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                    /**
                     * 原因: 不明
                     */
                    is ListCommentUseCase.Error.Unexpected -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("原因不明のエラーが発生しました"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(500)
                    )
                }
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
                when (val createdComment = createCommentUseCase.execute(slug, comment.body, authorizeResult.value)) {
                    /**
                     * コメントの登録に失敗
                     */
                    is Left -> when (val useCaseError = createdComment.value) {
                        /***
                         * 原因: Slug がバリデーションエラー
                         */
                        is CreateCommentUseCase.Error.InvalidSlug -> ResponseEntity(
                            serializeMyErrorListForResponseBody(useCaseError.errors),
                            HttpStatus.valueOf(422)
                        )
                        /***
                         * 原因: CommentBody がバリデーションエラー
                         */
                        is CreateCommentUseCase.Error.InvalidCommentBody -> ResponseEntity(
                            serializeMyErrorListForResponseBody(useCaseError.errors),
                            HttpStatus.valueOf(422)
                        )
                        /**
                         * 原因: 記事が見つかりませんでした
                         */
                        is CreateCommentUseCase.Error.NotFound -> ResponseEntity(
                            serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                            HttpStatus.valueOf(404)
                        )
                        /**
                         * 原因: 不明
                         */
                        is CreateCommentUseCase.Error.Unexpected -> ResponseEntity(
                            serializeUnexpectedErrorForResponseBody("原因不明のエラーが発生しました"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                            HttpStatus.valueOf(500)
                        )
                    }
                    /**
                     * コメントの登録に成功
                     * TODO authorId ではなく、author を戻すように修正する
                     */
                    is Right -> ResponseEntity(
                        Comment.from(createdComment.value).serializeWithRootName(),
                        HttpStatus.valueOf(200)
                    )
                }
            }
        }
    }

    @DeleteMapping("/articles/{slug}/comments/{id}")
    fun delete(
        @RequestHeader("Authorization") rawAuthorizationHeader: String?,
        @PathVariable("slug") slug: String?,
        @PathVariable("id") commentId: String?
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
                when (val result =
                    deleteCommentUseCase.execute(slug, NullableCommentId.from(commentId), authorizeResult.value)) {
                    /**
                     * コメントの削除に失敗
                     */
                    is Left -> when (val useCaseError = result.value) {
                        /**
                         * 原因: Slug がバリデーションエラー
                         */
                        is DeleteCommentUseCase.Error.InvalidSlug -> ResponseEntity(
                            serializeMyErrorListForResponseBody(useCaseError.errors),
                            HttpStatus.valueOf(422)
                        )
                        /**
                         * 原因: CommentId がバリデーションエラー
                         */
                        is DeleteCommentUseCase.Error.InvalidCommentId -> ResponseEntity(
                            serializeMyErrorListForResponseBody(useCaseError.errors),
                            HttpStatus.valueOf(422)
                        )
                        /**
                         * 原因: 記事が見つからなかった
                         */
                        is DeleteCommentUseCase.Error.ArticleNotFoundBySlug -> ResponseEntity(
                            serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                            HttpStatus.valueOf(404)
                        )
                        /**
                         * 原因: コメントが見つからなかった
                         */
                        is DeleteCommentUseCase.Error.CommentsNotFoundByCommentId -> ResponseEntity(
                            serializeUnexpectedErrorForResponseBody("コメントが見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                            HttpStatus.valueOf(404)
                        )
                        /**
                         * 原因: 未知のエラー
                         */
                        is DeleteCommentUseCase.Error.Unexpected -> ResponseEntity(
                            serializeUnexpectedErrorForResponseBody("原因不明のエラーが発生しました"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                            HttpStatus.valueOf(500)
                        )
                    }
                    /**
                     * コメントの削除に成功
                     */
                    is Right -> ResponseEntity(
                        "",
                        HttpStatus.valueOf(200)
                    )
                }
            }
        }
    }
}
