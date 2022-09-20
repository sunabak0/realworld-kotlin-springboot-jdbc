package com.example.realworldkotlinspringbootjdbc.usecase.article

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.FeedParameters
import com.example.realworldkotlinspringbootjdbc.usecase.shared_model.CreatedArticleWithAuthor
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service
import java.util.SortedSet

interface FeedUseCase {
    /**
     * 特定のユーザーのフォローしている人のそれぞれの最新記事一覧
     *
     * @property articles フィルタされた作成済み記事の一覧(最大数: limit)
     * @property articlesCount フィルタで引っかかった作成済み記事の数(メタデータ)
     */
    data class FeedCreatedArticles(
        val articles: SortedSet<CreatedArticleWithAuthor>,
        val articlesCount: Int,
    )

    fun execute(
        currentUser: RegisteredUser,
        limit: String? = null,
        offset: String? = null,
    ): Either<Error, FeedCreatedArticles> = throw UnsupportedOperationException()

    /**
     * - フィード用のバリデーションエラー
     * - Offset値がフィルタ済み作成済み記事の数を超過エラー
     */
    sealed interface Error : MyError {
        data class FeedParameterValidationErrors(
            override val errors: List<MyError.ValidationError>
        ) : Error, MyError.ValidationErrors

        data class OffsetOverCreatedArticlesCountError(
            val feedParameters: FeedParameters,
            val articlesCount: Int,
        ) : Error, MyError.Basic
    }
}

@Service
class FeedUseCaseImpl(
    val profileRepository: ProfileRepository,
    val articleRepository: ArticleRepository,
) : FeedUseCase {
    override fun execute(
        currentUser: RegisteredUser,
        limit: String?,
        offset: String?
    ): Either<FeedUseCase.Error, FeedUseCase.FeedCreatedArticles> {
        /**
         * フィード用パラメータ
         * バリデーションエラー -> 早期return
         */
        val feedParameters = FeedParameters.new(limit = limit, offset = offset).fold(
            { return FeedUseCase.Error.FeedParameterValidationErrors(errors = it).left() },
            { it }
        )

        /**
         * フォローしている登録済みユーザー郡
         * エラー -> ありえない
         */
        val followedUsers = profileRepository.filterFollowedByUser(currentUser.userId).fold(
            { throw UnsupportedOperationException("現在この分岐に入ることは無い") },
            { it }
        )

        /**
         * 指定した人の最新の作成済み記事郡
         * エラー -> ありえない
         * - 順番: 記事Id(昇順)
         */
        val latestArticlesWithAuthor =
            articleRepository.latestByAuthors(followedUsers.map { it.userId }.toSet(), currentUser.userId).fold(
                { throw UnsupportedOperationException("現在この分岐に入ることは無い") },
                {
                    it.map { article ->
                        CreatedArticleWithAuthor(
                            article = article,
                            author = followedUsers.find { user -> user.userId == article.authorId }!!
                        )
                    }.toSortedSet(compareBy { v -> v.article.id.value })
                }
            )

        /**
         * LimitとOffsetで制限
         */
        return when (latestArticlesWithAuthor.size < feedParameters.offset) {
            /**
             * Offset値が全体を超えてしまっている
             */
            true -> FeedUseCase.Error.OffsetOverCreatedArticlesCountError(
                feedParameters = feedParameters,
                articlesCount = latestArticlesWithAuthor.size,
            ).left()
            /**
             * Offset値分ずらせる
             */
            false -> FeedUseCase.FeedCreatedArticles(
                articles = latestArticlesWithAuthor
                    .toList<CreatedArticleWithAuthor>()
                    .slice(feedParameters.offset until latestArticlesWithAuthor.size)
                    .take(feedParameters.limit)
                    .toSortedSet(compareBy { it.article.id.value }),
                articlesCount = latestArticlesWithAuthor.size,
            ).right()
        }
    }
}
