package com.example.realworldkotlinspringbootjdbc.usecase.comment

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.CommentRepository
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface DeleteCommentUseCase {
    fun execute(slug: String?, commentId: Int?, currentUser: RegisteredUser): Either<Error, Unit> =
        throw NotImplementedError()

    sealed interface Error : MyError {
        data class InvalidSlug(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class InvalidCommentId(override val errors: List<MyError.ValidationError>) :
            Error,
            MyError.ValidationErrors

        data class NotFoundArticleBySlug(override val cause: MyError, val slug: Slug) :
            Error,
            MyError.MyErrorWithMyError

        data class NotFoundCommentByCommentId(override val cause: MyError, val commentId: CommentId) :
            Error,
            MyError.MyErrorWithMyError

        data class NotAuthorizedDeleteComment(override val cause: MyError, val currentUserId: UserId) :
            Error,
            MyError.MyErrorWithMyError
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
        /**
         * Slug のバリデーション
         * Invalid -> 早期リターン
         */
        val validatedSlug = Slug.new(slug).fold(
            { return DeleteCommentUseCase.Error.InvalidSlug(it).left() },
            { it }
        )

        /**
         * CommentId のバリデーション
         * Invalid -> 早期リターン
         */
        val validateCommentId = CommentId.new(commentId).fold(
            { return DeleteCommentUseCase.Error.InvalidCommentId(it).left() },
            { it }
        )

        /**
         * コメント削除
         */
        return when (
            val deleteResult =
                commentRepository.delete(validatedSlug, validateCommentId, currentUser.userId)
        ) {
            /**
             * コメント削除 失敗
             */
            is Left -> when (val error = deleteResult.value) {
                /**
                 * 原因: Slug に該当する記事が見つからなかった
                 */
                is CommentRepository.DeleteError.NotFoundArticleBySlug -> DeleteCommentUseCase.Error.NotFoundArticleBySlug(
                    error,
                    validatedSlug
                ).left()
                /**
                 * 原因: CommentId に該当するコメントが見つからなかった
                 */
                is CommentRepository.DeleteError.NotFoundCommentByCommentId -> DeleteCommentUseCase.Error.NotFoundCommentByCommentId(
                    error,
                    validateCommentId
                ).left()
                /**
                 * 原因: 認可されていない（削除しようとしたが実行ユーザー（CurrentUserId）のものじゃなかった）
                 */
                is CommentRepository.DeleteError.NotAuthorizedDeleteComment -> DeleteCommentUseCase.Error.NotAuthorizedDeleteComment(
                    error,
                    currentUser.userId
                ).left()
            }
            /**
             * コメント削除 成功
             */
            is Right -> deleteResult
        }
    }
}
