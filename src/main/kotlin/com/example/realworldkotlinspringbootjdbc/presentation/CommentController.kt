package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Some
import arrow.core.none
import com.example.realworldkotlinspringbootjdbc.openapi.generated.controller.CommentsApi
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModel
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModelErrors
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.NewCommentRequest
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Profile
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.SingleCommentResponse
import com.example.realworldkotlinspringbootjdbc.presentation.response.Comment
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
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
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneOffset

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

    override fun createArticleComment(
        authorization: String,
        slug: String,
        comment: NewCommentRequest
    ): ResponseEntity<SingleCommentResponse> {
        val currenUser = realworldAuthenticationUseCase.execute(authorization).fold(
            { throw RealworldAuthenticationUseCaseUnauthorizedException(it) },
            { it }
        )

        val commentWithOtherUser = createCommentUseCase.execute(slug, comment.comment.body, currenUser).fold(
            { throw CreateCommentUseCaseErrorException(it) },
            { it }
        )

        return ResponseEntity(
            SingleCommentResponse(
                com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Comment(
                    id = commentWithOtherUser.comment.id.value,
                    createdAt = commentWithOtherUser.comment.createdAt.toInstant().atOffset(ZoneOffset.UTC),
                    updatedAt = commentWithOtherUser.comment.updatedAt.toInstant().atOffset(ZoneOffset.UTC),
                    body = commentWithOtherUser.comment.body.value,
                    Profile(
                        username = commentWithOtherUser.author.username.value,
                        bio = commentWithOtherUser.author.bio.value,
                        image = commentWithOtherUser.author.image.value,
                        following = commentWithOtherUser.author.following
                    )
                )
            ),
            HttpStatus.OK
        )
    }

    data class CreateCommentUseCaseErrorException(val error: CreateCommentUseCase.Error) : Exception()

    @ExceptionHandler(value = [CreateCommentUseCaseErrorException::class])
    fun onCreateCommentUseCaseErrorException(e: CreateCommentUseCaseErrorException): ResponseEntity<GenericErrorModel> =
        when (val error = e.error) {
            /**
             * 原因: CommentBody がバリデーションエラー
             */
            is CreateCommentUseCase.Error.InvalidCommentBody -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = error.errors.map { it.message })),
                HttpStatus.UNPROCESSABLE_ENTITY
            )
            /**
             * 原因: Slug がバリデーションエラー
             */
            is CreateCommentUseCase.Error.InvalidSlug -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("slug が不正です"))),
                HttpStatus.UNPROCESSABLE_ENTITY
            )
            /**
             * 原因: 記事が見つかりませんでした
             */
            is CreateCommentUseCase.Error.NotFound -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("記事が見つかりませんでした"))),
                HttpStatus.NOT_FOUND
            )
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
