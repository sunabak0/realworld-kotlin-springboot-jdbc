package com.example.realworldkotlinspringbootjdbc.usecase.comment

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.CommentRepository
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface DeleteCommentUseCase {
    fun execute(slug: String?, commentId: Int?, currentUser: RegisteredUser): Either<Error, Unit> = TODO()

    sealed interface Error : MyError {
        data class InvalidSlug(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class InvalidCommentId(override val errors: List<MyError.ValidationError>) :
            Error,
            MyError.ValidationErrors

        data class ArticleNotFoundBySlug(override val cause: MyError, val slug: Slug) :
            Error,
            MyError.MyErrorWithMyError

        data class CommentsNotFoundByCommentId(override val cause: MyError, val commentId: CommentId) :
            Error,
            MyError.MyErrorWithMyError

        data class Unexpected(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class DeleteCommentUseCaseImpl(
    val commentRepository: CommentRepository
) : DeleteCommentUseCase {
    override fun execute(
        slug: String?,
        commentId: Int?,
        currentUser: RegisteredUser
    ): Either<DeleteCommentUseCase.Error, Unit> {
        return when (val slugResult = Slug.new(slug)) {
            /**
             * Slug が不正
             */
            is Invalid -> DeleteCommentUseCase.Error.InvalidSlug(slugResult.value).left()
            /**
             * Slug が適切
             */
            is Valid -> when (val commentIdResult = CommentId.new(commentId)) {
                /**
                 * CommentId が不正
                 */
                is Invalid -> DeleteCommentUseCase.Error.InvalidCommentId(commentIdResult.value).left()
                /**
                 * CommentId が適切
                 */
                is Valid -> when (
                    val deleteResult =
                        commentRepository.delete(slugResult.value, commentIdResult.value, currentUser.userId)
                ) {
                    /**
                     * コメント削除 失敗
                     */
                    is Left -> when (val error = deleteResult.value) {
                        /**
                         * 原因: Slug に該当する記事が見つからなかった
                         */
                        is CommentRepository.DeleteError.ArticleNotFoundBySlug -> DeleteCommentUseCase.Error.ArticleNotFoundBySlug(
                            error,
                            slugResult.value,
                        ).left()
                        /**
                         * 原因: CommentId に該当するコメントが見つからなかった
                         */
                        is CommentRepository.DeleteError.CommentNotFoundByCommentId -> DeleteCommentUseCase.Error.CommentsNotFoundByCommentId(
                            error,
                            commentIdResult.value
                        ).left()
                        /**
                         * 原因: 不明
                         */
                        is CommentRepository.DeleteError.Unexpected -> DeleteCommentUseCase.Error.Unexpected(error)
                            .left()
                    }
                    /**
                     * コメント削除 成功
                     */
                    is Right -> deleteResult
                }
            }
        }
    }
}
