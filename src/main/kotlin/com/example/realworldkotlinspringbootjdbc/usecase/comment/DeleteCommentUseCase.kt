package com.example.realworldkotlinspringbootjdbc.usecase.comment

import arrow.core.Either
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
    override fun execute(slug: String?, commentId: Int?, currentUser: RegisteredUser): Either<DeleteCommentUseCase.Error, Unit> {
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
                is Valid -> TODO()
            }
        }
    }
}
