package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Some
import arrow.core.none
import arrow.core.toOption
import com.example.realworldkotlinspringbootjdbc.openapi.generated.controller.CommentsApi
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Comment
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModel
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModelErrors
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.MultipleCommentsResponse
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.NewCommentRequest
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Profile
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.SingleCommentResponse
import com.example.realworldkotlinspringbootjdbc.presentation.shared.RealworldAuthenticationUseCaseUnauthorizedException
import com.example.realworldkotlinspringbootjdbc.usecase.comment.CreateCommentUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.comment.DeleteCommentUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.comment.ListCommentUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.shared.RealworldAuthenticationUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
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
    override fun getArticleComments(slug: String, authorization: String?): ResponseEntity<MultipleCommentsResponse> {
        val currentUser = authorization.toOption().fold(
            { none() },
            {
                realworldAuthenticationUseCase.execute(it).fold(
                    { none() },
                    { result -> Some(result) }
                )
            }
        )

        val commentWithAuthors = listCommentUseCase.execute(slug, currentUser).fold(
            { throw ListCommentUseCaseErrorException(it) },
            { it }
        )

        return ResponseEntity(
            MultipleCommentsResponse(
                commentWithAuthors.map {
                    Comment(
                        id = it.comment.id.value,
                        createdAt = it.comment.createdAt.toInstant().atOffset(ZoneOffset.UTC),
                        updatedAt = it.comment.updatedAt.toInstant().atOffset(ZoneOffset.UTC),
                        body = it.comment.body.value,
                        Profile(
                            username = it.author.username.value,
                            bio = it.author.bio.value,
                            image = it.author.image.value,
                            following = it.author.following
                        )
                    )
                }
            ),
            HttpStatus.OK
        )
    }

    data class ListCommentUseCaseErrorException(val error: ListCommentUseCase.Error) : Exception()

    @ExceptionHandler(value = [ListCommentUseCaseErrorException::class])
    fun onListCommentUseCaseErrorException(e: ListCommentUseCaseErrorException): ResponseEntity<GenericErrorModel> =
        when (val error = e.error) {
            is ListCommentUseCase.Error.InvalidSlug -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("記事が見つかりませんでした"))),
                HttpStatus.NOT_FOUND
            )

            is ListCommentUseCase.Error.NotFound -> TODO()
        }

    // override fun getArticleComments(authorization: String, slug: String): ResponseEntity<MultipleCommentsResponse> {
    //     val currentUser = realworldAuthenticationUseCase.execute(authorization).fold(
    //         { none() },
    //         { Some(it) }
    //     )
    //
    //     val createdArticleWithAuthors = listCommentUseCase.execute(slug, currentUser).fold(
    //         { throw TODO() },
    //         { it }
    //     )
    //
    //     // TODO: UseCase の実装が終わるまで、一時的に author object を dummy データに設定
    //     return ResponseEntity(
    //         MultipleCommentsResponse(
    //             createdArticleWithAuthors.map {
    //                 Comment(
    //                     id = it.id.value,
    //                     createdAt = it.createdAt.toInstant().atOffset(ZoneOffset.UTC),
    //                     updatedAt = it.updatedAt.toInstant().atOffset(ZoneOffset.UTC),
    //                     body = it.body.value,
    //                     Profile(
    //                         username = "dummy-username",
    //                         bio = "dummy-bio",
    //                         image = "dummy-image",
    //                         following = false
    //                     )
    //                 )
    //             }
    //         ),
    //         HttpStatus.OK
    //     )
    // }

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
