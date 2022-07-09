package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.CommentRepository
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface ListCommentUseCase {
    fun execute(slug: String?): Either<Error, List<Comment>> = TODO()
    sealed interface Error : MyError {
        data class InvalidSlug(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class NotFound(override val cause: MyError) : Error, MyError.MyErrorWithMyError
        data class Unexpected(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class ShowCommentUseCaseImpl(
    val commentRepository: CommentRepository
) : ListCommentUseCase {
    override fun execute(slug: String?): Either<ListCommentUseCase.Error, List<Comment>> {
        return when (val it = Slug.new(slug)) {
            is Invalid -> ListCommentUseCase.Error.InvalidSlug(it.value).left()
            is Valid -> when (val listResult = commentRepository.list(it.value)) {
                /**
                 * コメントの取得 成功
                 */
                is Right -> listResult.value.right()
                /**
                 * コメントの取得失敗
                 */
                is Left -> when (val listError = listResult.value) {
                    /**
                     * 原因: Slug に該当する記事が見つからなかった
                     */
                    is CommentRepository.ListError.NotFoundArticleBySlug -> ListCommentUseCase.Error.NotFound(listError)
                        .left()
                    /**
                     * 原因: 不明
                     */
                    is CommentRepository.ListError.Unexpected -> ListCommentUseCase.Error.Unexpected(listError).left()
                }
            }
        }
    }
}
