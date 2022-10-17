package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Some
import arrow.core.none
import com.example.realworldkotlinspringbootjdbc.openapi.generated.controller.CommentsApi
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModel
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModelErrors
import com.example.realworldkotlinspringbootjdbc.presentation.request.NullableComment
import com.example.realworldkotlinspringbootjdbc.presentation.response.Comment
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeMyErrorListForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.shared.AuthorizationError
import com.example.realworldkotlinspringbootjdbc.presentation.shared.RealworldAuthenticationUseCaseUnauthorizedException
import com.example.realworldkotlinspringbootjdbc.usecase.comment.CreateCommentUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.comment.DeleteCommentUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.comment.ListCommentUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.shared.RealworldAuthenticationUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class CommentController(
    val myAuth: MyAuth,
    val realworldAuthenticationUseCase: RealworldAuthenticationUseCase,
    val listCommentUseCase: ListCommentUseCase,
    val createCommentUseCase: CreateCommentUseCase,
    val deleteCommentUseCase: DeleteCommentUseCase
) : CommentsApi {
    @GetMapping("/articles/{slug}/comments")
    fun list(
        @RequestHeader("Authorization") rawAuthorizationHeader: String?,
        @PathVariable("slug") slug: String?
    ): ResponseEntity<String> {
        val optionalCurrentUser = myAuth.authorize(rawAuthorizationHeader).fold(
            { none() },
            { Some(it) }
        )
        return when (
            val useCaseResult = listCommentUseCase.execute(slug, optionalCurrentUser)
        ) {
            /**
             * コメント取得に失敗
             */
            is Left -> when (useCaseResult.value) {
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
            }
            /**
             * コメント取得に成功
             * TODO authorId ではなく、author を戻すように修正する
             */
            is Right -> {
                val comments =
                    useCaseResult.value.map {
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
            is Left -> AuthorizationError.handle()
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

    override fun deleteArticleComment(authorization: String, slug: String, id: Int): ResponseEntity<Unit> {
        val currentUser = realworldAuthenticationUseCase.execute(authorization).fold(
            { throw RealworldAuthenticationUseCaseUnauthorizedException(it) },
            { it }
        )

        deleteCommentUseCase.execute(slug, id, currentUser).fold(
            { throw DeleteCommentUseCaseErrorException(it) },
            {}
        )

        return ResponseEntity(
            HttpStatus.OK
        )
    }

    data class DeleteCommentUseCaseErrorException(val error: DeleteCommentUseCase.Error) : Exception()

    @ExceptionHandler(value = [DeleteCommentUseCaseErrorException::class])
    fun onDeleteCommentUseCaseErrorException(e: DeleteCommentUseCaseErrorException): ResponseEntity<GenericErrorModel> =
        when (e.error) {
            /**
             * 原因: CommentId がバリデーションエラー
             */
            is DeleteCommentUseCase.Error.InvalidCommentId -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("コメント ID が不正です"))),
                HttpStatus.UNPROCESSABLE_ENTITY
            )
            /**
             * 原因: Slug がバリデーションエラー
             */
            is DeleteCommentUseCase.Error.InvalidSlug -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("Slug が不正です"))),
                HttpStatus.UNPROCESSABLE_ENTITY
            )
            /**
             * 原因: 認可されていない（削除しようとしたが実行ユーザー（CurrentUserId）のものじゃなかった）
             */
            is DeleteCommentUseCase.Error.NotAuthorizedDeleteComment -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("コメントの削除が許可されていません"))),
                HttpStatus.UNAUTHORIZED
            )
            /**
             * 原因: 記事が見つからなかった
             */
            is DeleteCommentUseCase.Error.NotFoundArticleBySlug -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("記事が見つかりませんでした"))),
                HttpStatus.NOT_FOUND
            )
            /**
             * 原因: CommentId に該当するコメントがなかった
             */
            is DeleteCommentUseCase.Error.NotFoundCommentByCommentId -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("コメントが見つかりませんでした"))),
                HttpStatus.NOT_FOUND
            )
        }
}
