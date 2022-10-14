package com.example.realworldkotlinspringbootjdbc.usecase.comment

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.CommentRepository
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface ListCommentUseCase {
    fun execute(slug: String?, currentUser: Option<RegisteredUser> = None): Either<Error, List<Comment>> =
        throw NotImplementedError()

    sealed interface Error : MyError {
        data class InvalidSlug(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class NotFound(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class ListCommentUseCaseImpl(
    val commentRepository: CommentRepository
) : ListCommentUseCase {
    override fun execute(
        slug: String?,
        currentUser: Option<RegisteredUser>
    ): Either<ListCommentUseCase.Error, List<Comment>> {
        /**
         * Slug のバリデーション
         * Invalid -> 早期リターン
         */
        val validatedSlug = Slug.new(slug).fold(
            { return ListCommentUseCase.Error.InvalidSlug(it).left() },
            { it }
        )
        return when (val listResult = commentRepository.list(validatedSlug)) {
            /**
             * コメントの取得 失敗
             */
            is Left -> when (val listError = listResult.value) {
                is CommentRepository.ListError.NotFoundArticleBySlug -> ListCommentUseCase.Error.NotFound(
                    listError
                ).left()
            }
            /**
             * コメントの取得 成功
             */
            is Right -> when (currentUser) {
                /**
                 * JWT 認証 失敗 or 未ログイン
                 * TODO: QueryService で AuthorId に該当する User を取得する実装を追加。現状は listResult（List<Comment>）を返している
                 */
                is None -> listResult
                /**
                 * JWT 認証成功
                 * TODO: QueryService で AuthorId に該当する User を取得する実装と AuthorId と CurrentUser の followings を取得する実装を追加。現状は listResult（List<Comment>）を返している
                 */
                is Some -> listResult
            }
        }
    }
}
