package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Invalid
import arrow.core.Valid
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.CommentRepository
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface CreateCommentsUseCase {
    fun execute(slug: String?, body: String?): Either<Error, Comment> = TODO()
    interface Error : MyError {
        data class InvalidSlug(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class InvalidCommentBody(override val errors: List<MyError.ValidationError>) : Error,
            MyError.ValidationErrors

        data class NotFound(override val cause: MyError) : Error, MyError.MyErrorWithMyError
        data class Unexpected(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class CreateCommentsUseCaseImpl(
    val commentRepository: CommentRepository
) : CreateCommentsUseCase {
    override fun execute(slug: String?, body: String?): Either<CreateCommentsUseCase.Error, Comment> {
        return when (val it = Slug.new(slug)) {
            /**
             * Slug が不正
             */
            is Invalid -> CreateCommentsUseCase.Error.InvalidSlug(it.value).left()
            /**
             * Slug が適切
             */
            is Valid -> when (val commentBody = Body.new(body)) {
                /**
                 * CommentBody が不正
                 */
                is Invalid -> CreateCommentsUseCase.Error.InvalidCommentBody(commentBody.value).left()
                /**
                 * CommentBody が適切
                 */
                is Valid -> when (val createResult = commentRepository.create(commentBody.value)) {
                    /**
                     * コメント登録成功
                     */
                    is Left -> TODO()
                    /**
                     * コメント登録失敗
                     */
                    is Right -> TODO()
                }
            }
        }
    }
}
