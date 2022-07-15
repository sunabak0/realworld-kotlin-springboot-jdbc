package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.CommentRepository
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface ListCommentUseCase {
    fun execute(slug: String?, currentUser: Option<RegisteredUser> = None): Either<Error, List<Comment>> = TODO()
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
    override fun execute(
        slug: String?,
        currentUser: Option<RegisteredUser>
    ): Either<ListCommentUseCase.Error, List<Comment>> {
        return when (val it = Slug.new(slug)) {
            /**
             * Slug が不正
             */
            is Invalid -> ListCommentUseCase.Error.InvalidSlug(it.value).left()
            /**
             * Slug が適切
             */
            is Valid -> when (val listResult = commentRepository.list(it.value)) {
                /**
                 * コメントの取得失敗
                 */
                is Left -> when (val listError = listResult.value) {
                    /**
                     * 原因: Slug に該当する記事が見つからなかった
                     */
                    is CommentRepository.ListError.NotFoundArticleBySlug -> ListCommentUseCase.Error.NotFound(
                        listError
                    ).left()
                    /**
                     * 原因: 不明
                     */
                    is CommentRepository.ListError.Unexpected -> ListCommentUseCase.Error.Unexpected(
                        listError
                    ).left()
                }
                /**
                 * コメントの取得 成功
                 * TODO: listResult が空（List<Comment> = []）のとき、早期 return を実装するか検討
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
}
