package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface DeleteCommentsUseCase {
    fun execute(slug: String?, commentId: Int?): Either<Error, Unit> = TODO()

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
class DeleteCommentsUseCaseImpl : DeleteCommentsUseCase {

}
