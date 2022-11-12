package com.example.realworldkotlinspringbootjdbc.usecase.favorite

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface UnfavoriteUseCase {
    fun execute(slug: String?, currentUser: RegisteredUser): Either<Error, CreatedArticle> = throw NotImplementedError()
    sealed interface Error : MyError {
        data class InvalidSlug(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class NotFound(override val cause: MyError) :
            Error,
            MyError.MyErrorWithMyError
    }
}

@Service
class UnfavoriteUseCaseImpl(
    val articleRepository: ArticleRepository
) : UnfavoriteUseCase {
    override fun execute(slug: String?, currentUser: RegisteredUser): Either<UnfavoriteUseCase.Error, CreatedArticle> {
        /**
         * Slug のバリデーション
         * Invalid -> 早期 return
         */
        val validatedSlug = Slug.new(slug).fold(
            { return UnfavoriteUseCase.Error.InvalidSlug(it).left() },
            { it }
        )

        return when (val unfavoriteResult = articleRepository.unfavorite(validatedSlug, currentUser.userId)) {
            /**
             * お気に入り登録解除 失敗
             */
            is Left -> when (val error = unfavoriteResult.value) {
                /**
                 * 原因: 作成済記事が見つからなかった
                 */
                is ArticleRepository.UnfavoriteError.NotFoundCreatedArticleBySlug -> UnfavoriteUseCase.Error.NotFound(
                    error
                ).left()
            }
            /**
             * お気に入り登録解除 成功
             */
            is Right -> unfavoriteResult
        }
    }
}
