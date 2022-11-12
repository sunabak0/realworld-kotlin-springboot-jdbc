package com.example.realworldkotlinspringbootjdbc.usecase.favorite

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.usecase.shared_model.CreatedArticleWithAuthor
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface FavoriteUseCase {
    fun execute(slug: String, currentUser: RegisteredUser): Either<Error, CreatedArticleWithAuthor> =
        throw NotImplementedError()

    sealed interface Error : MyError {
        data class InvalidSlug(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class NotFound(override val cause: MyError) :
            Error,
            MyError.MyErrorWithMyError
    }
}

@Service
class FavoriteUseCaseImpl(
    val articleRepository: ArticleRepository,
    val profileRepository: ProfileRepository
) : FavoriteUseCase {
    override fun execute(
        slug: String,
        currentUser: RegisteredUser
    ): Either<FavoriteUseCase.Error, CreatedArticleWithAuthor> {
        /**
         * Slug のバリデーション
         * Invalid -> 早期リターン
         */
        val validatedSlug = Slug.new(slug).fold(
            { return FavoriteUseCase.Error.InvalidSlug(it).left() },
            { it }
        )

        val favoriteResult = when (val favoriteResult = articleRepository.favorite(validatedSlug, currentUser.userId)) {
            /**
             * お気に入り追加 失敗
             */
            is Left -> when (val favoriteError = favoriteResult.value) {
                /**
                 * 原因: 作成済記事が見つからなかった
                 */
                is ArticleRepository.FavoriteError.NotFoundCreatedArticleBySlug -> return FavoriteUseCase.Error.NotFound(
                    favoriteError
                ).left()
            }
            /**
             * お気に入り追加 成功
             */
            is Right -> favoriteResult
        }

        /**
         * フォローしている登録済みユーザー郡
         * エラー -> ありえない
         */
        val followedUsers = profileRepository.filterFollowedByUser(currentUser.userId).fold(
            { throw UnsupportedOperationException("現在この分岐に入ることは無い") },
            { it }
        )

        /**
         * author を followedUsers から取得
         */
        return CreatedArticleWithAuthor(
            article = favoriteResult.value,
            author = followedUsers.find { user -> user.userId == favoriteResult.value.authorId }!!
        ).right()
    }
}
