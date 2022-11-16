package com.example.realworldkotlinspringbootjdbc.usecase.favorite

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.usecase.shared_model.CreatedArticleWithAuthor
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface UnfavoriteUseCase {
    fun execute(slug: String?, currentUser: RegisteredUser): Either<Error, CreatedArticleWithAuthor> =
        throw NotImplementedError()

    sealed interface Error : MyError {
        data class InvalidSlug(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class NotFound(override val cause: MyError) :
            Error,
            MyError.MyErrorWithMyError
    }
}

@Service
class UnfavoriteUseCaseImpl(
    val articleRepository: ArticleRepository,
    val profileRepository: ProfileRepository
) : UnfavoriteUseCase {
    override fun execute(
        slug: String?,
        currentUser: RegisteredUser
    ): Either<UnfavoriteUseCase.Error, CreatedArticleWithAuthor> {
        /**
         * Slug のバリデーション
         * Invalid -> 早期 return
         */
        val validatedSlug = Slug.new(slug).fold(
            { return UnfavoriteUseCase.Error.InvalidSlug(it).left() },
            { it }
        )

        /**
         * お気に入り登録解除
         */
        val unfavoriteResult =
            when (val unfavoriteResult = articleRepository.unfavorite(validatedSlug, currentUser.userId)) {
                /**
                 * お気に入り登録解除 失敗
                 */
                is Left -> return when (val error = unfavoriteResult.value) {
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

        /**
         * author を取得
         * 必ず 1 件見つかるため、first を指定している
         */
        val author =
            profileRepository.filterByUserIds(setOf(unfavoriteResult.value.authorId), currentUser.userId.toOption())
                .getOrHandle { throw UnsupportedOperationException("現在この分岐に入ることは無い") }.first()

        /**
         * author を followedUsers から取得
         */
        return CreatedArticleWithAuthor(
            article = unfavoriteResult.value,
            author = author
        ).right()
    }
}
