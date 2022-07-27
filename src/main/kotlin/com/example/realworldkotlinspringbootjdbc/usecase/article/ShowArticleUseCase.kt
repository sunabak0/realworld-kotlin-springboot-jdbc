package com.example.realworldkotlinspringbootjdbc.usecase.article

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface ShowArticleUseCase {
    fun execute(slug: String?, currentUser: Option<RegisteredUser> = None): Either<Error, CreatedArticle> = TODO()
    sealed interface Error : MyError {
        data class ValidationErrors(override val errors: List<MyError.ValidationError>) :
            Error,
            MyError.ValidationErrors

        data class NotFoundArticleBySlug(override val cause: MyError, val slug: Slug) : Error,
            MyError.MyErrorWithMyError

        data class NotFoundUser(override val cause: MyError, val user: RegisteredUser) : Error,
            MyError.MyErrorWithMyError

        data class Unexpected(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class ShowArticleUseCaseImpl(val articleRepository: ArticleRepository) : ShowArticleUseCase {
    override fun execute(
        slug: String?,
        currentUser: Option<RegisteredUser>
    ): Either<ShowArticleUseCase.Error, CreatedArticle> {
        return when (val it = Slug.new(slug)) {
            is Invalid -> ShowArticleUseCase.Error.ValidationErrors(it.value).left()
            is Valid -> when (currentUser) {
                /**
                 * JWT 認証失敗 or 未ログイン
                 */
                is None -> when (val createdArticle = articleRepository.findBySlug(it.value)) {
                    /**
                     * 作成済記事取得 失敗
                     */
                    is Left -> when (val error = createdArticle.value) {
                        /**
                         * 原因: slug に該当する記事が見つからなかった
                         */
                        is ArticleRepository.FindBySlugError.NotFound -> ShowArticleUseCase.Error.NotFoundArticleBySlug(
                            error,
                            it.value
                        )
                            .left()
                        /**
                         * 原因: 不明
                         */
                        is ArticleRepository.FindBySlugError.Unexpected -> ShowArticleUseCase.Error.Unexpected(error)
                            .left()
                    }
                    /**
                     * 作成済記事取得 成功
                     */
                    is Right -> createdArticle
                }
                /**
                 * JWT 認証成功
                 */
                is Some -> when (val createdArticle =
                    articleRepository.findBySlugFromRegisteredUserViewpoint(it.value, currentUser.value.userId)) {
                    /**
                     * 作成済記事取得 失敗
                     */
                    is Left -> when (val error = createdArticle.value) {
                        /**
                         * 原因: slug に該当する記事が見つからなかった
                         */
                        is ArticleRepository.FindBySlugFromRegisteredUserViewpointError.NotFoundArticle -> ShowArticleUseCase.Error.NotFoundArticleBySlug(
                            error,
                            it.value
                        ).left()
                        /**
                         * 原因: ユーザーがいなかった
                         */
                        is ArticleRepository.FindBySlugFromRegisteredUserViewpointError.NotFoundUser -> ShowArticleUseCase.Error.NotFoundUser(
                            error,
                            currentUser.value
                        ).left()
                        /**
                         * 原因: 不明
                         */
                        is ArticleRepository.FindBySlugFromRegisteredUserViewpointError.Unexpected -> ShowArticleUseCase.Error.Unexpected(
                            error
                        ).left()
                    }
                    /**
                     * 作成済記事取得 成功
                     */
                    is Right -> createdArticle
                }
            }
        }
    }
}
