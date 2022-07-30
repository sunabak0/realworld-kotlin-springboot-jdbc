package com.example.realworldkotlinspringbootjdbc.usecase.favorite

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Invalid
import arrow.core.Valid
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.FavoriteRepository
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface FavoriteUseCase {
    fun execute(slug: String?, currentUser: RegisteredUser): Either<Error, CreatedArticle> = TODO()
    sealed interface Error : MyError {
        data class InvalidSlug(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class ArticleNotFoundBySlug(override val cause: MyError, val slug: Slug) :
            Error,
            MyError.MyErrorWithMyError

        data class Unexpected(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class FavoriteUseCaseImpl(
    val favoriteRepository: FavoriteRepository
) : FavoriteUseCase {
    override fun execute(slug: String?, currentUser: RegisteredUser): Either<FavoriteUseCase.Error, CreatedArticle> {
        return when (val it = Slug.new(slug)) {
            /**
             * Slug が不正
             */
            is Invalid -> FavoriteUseCase.Error.InvalidSlug(it.value).left()
            /**
             * Slug が適切
             */
            is Valid -> when (val favoriteResult = favoriteRepository.favorite(it.value, currentUser.userId)) {
                /**
                 * お気に入り追加 成功
                 */
                is Left -> when (favoriteResult.value) {
                    /**
                     * 原因: 作成済記事が見つからなかった
                     */
                    is FavoriteRepository.FavoriteError.ArticleNotFoundBySlug -> TODO()
                    /**
                     * 原因: 不明
                     */
                    is FavoriteRepository.FavoriteError.Unexpected -> TODO()
                }
                /**
                 * お気に入り追加 失敗
                 */
                is Right -> favoriteResult
            }
        }
    }
}
