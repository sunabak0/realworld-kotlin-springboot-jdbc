package com.example.realworldkotlinspringbootjdbc.usecase.favorite

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Invalid
import arrow.core.Valid
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.FavoriteRepository
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface UnfavoriteUseCase {
    fun execute(slug: String?, currentUser: RegisteredUser): Either<Error, Unit> = TODO()
    sealed interface Error : MyError {
        data class InvalidSlug(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class ArticleNotFoundBySlug(override val cause: MyError, val slug: Slug) :
            Error,
            MyError.MyErrorWithMyError

        data class Unexpected(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class UnfavoriteUseCaseImpl(
    val favoriteRepository: FavoriteRepository
) : UnfavoriteUseCase {
    override fun execute(slug: String?, currentUser: RegisteredUser): Either<UnfavoriteUseCase.Error, Unit> {
        return when (val it = Slug.new(slug)) {
            /**
             * Slug が不正
             */
            is Invalid -> UnfavoriteUseCase.Error.InvalidSlug(it.value).left()
            /**
             * Slug が適切
             */
            is Valid -> when (val unfavoriteResult = favoriteRepository.unfavorite(it.value, currentUser.userId)) {
                /**
                 * お気に入り登録解除 失敗
                 */
                is Left -> when (unfavoriteResult.value) {
                    /**
                     * 原因: 作成済記事が見つからなかった
                     */
                    is FavoriteRepository.UnfavoriteError.ArticleNotFoundBySlug -> TODO()
                    /**
                     * 原因: 不明
                     */
                    is FavoriteRepository.UnfavoriteError.Unexpected -> TODO()
                }
                /**
                 * お気に入り登録解除 成功
                 */
                is Right -> unfavoriteResult
            }
        }
    }
}
