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
import arrow.core.none
import arrow.core.right
import arrow.core.toOption
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.FilterParameters
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.usecase.shared_model.CreatedArticleWithAuthor
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface FilterCreatedArticleUseCase {
    /**
     * フィルタされた作成済み記事一覧
     *
     * @property articles フィルタされた作成済み記事の一覧(最大数: limit)
     * @property articlesCount フィルタで引っかかった作成済み記事の数(メタデータ)
     */
    data class FilteredCreatedArticleList(
        val articles: List<CreatedArticleWithAuthor>,
        val articlesCount: Int,
    )

    /**
     * 作成済み記事をフィルタ(フィルタがなければただの一覧取得)
     *
     * @param tag タグでフィルタ(1つだけ、複数ではできない)
     * @param author 作成済み記事の著者でフィルタ(ユーザー名, 1人分だけ)
     * @param favoritedByUsernamea 特定の登録済みユーザーがお気に入りしているかどうかでフィルタ(ユーザー名, 1人分だけ)
     * @param limit 1度に表示する表示数
     * @param offset 作成済み記事N個のうち、Offset個分だけskipしてそこから表示(0の場合、0個分skip)
     * @return エラー or 作成済み記事一覧
     */
    fun execute(
        tag: String? = null,
        author: String? = null,
        favoritedByUsername: String? = null,
        limit: String? = null,
        offset: String? = null,
        currentUser: Option<RegisteredUser> = none()
    ): Either<Error, FilteredCreatedArticleList> = throw NotImplementedError()

    /**
     * - フィルタ用パラメータのバリデーションエラー
     * - Offset値がフィルタ済み作成済み記事の数を超過エラー
     */
    sealed interface Error : MyError {
        data class FilterParametersValidationErrors(override val errors: List<MyError.ValidationError>) :
            Error,
            MyError.ValidationErrors

        data class OffsetOverCreatedArticlesCountError(
            val filterParameters: FilterParameters,
            val articlesCount: Int,
        ) : Error, MyError.Basic

        // TODO: いらないかも:要検討
        data class NotFoundUser(
            val user: RegisteredUser,
            override val cause: MyError
        ) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class FilterCreatedArticleUseCaseImpl(
    val articleRepository: ArticleRepository,
    val profileRepository: ProfileRepository,
) : FilterCreatedArticleUseCase {
    /**
     * 1. フィルタパラメータ
     * 2. 誰かのお気に入りの作成済み記事フィルタ or 全作成済み記事
     * 3. 作成済み記事にAuthor情報を付与
     * 4. 著者名やタグでフィルタ
     * 5. LimitとOffsetで返すListを調整
     */
    override fun execute(
        tag: String?,
        author: String?,
        favoritedByUsername: String?,
        limit: String?,
        offset: String?,
        currentUser: Option<RegisteredUser>
    ): Either<FilterCreatedArticleUseCase.Error, FilterCreatedArticleUseCase.FilteredCreatedArticleList> {
        /**
         * 1. フィルタパラメータ
         */
        val filterParameters = when (
            val filterParameters = FilterParameters.new(
                tag = tag,
                author = author,
                favoritedByUsername = favoritedByUsername,
                limit = limit,
                offset = offset,
            )
        ) {
            is Invalid -> return FilterCreatedArticleUseCase.Error.FilterParametersValidationErrors(filterParameters.value).left()
            is Valid -> filterParameters.value
        }

        /**
         * 2. 誰かのお気に入りの作成済み記事フィルタ or 全作成済み記事
         */
        val filterTargetBase = when (val optionalUserName = filterParameters.favoritedByUsername) {
            /**
             * 特定の他ユーザーがお気に入りしているフィルタ 無し
             * - 全ての作成済み記事
             */
            None -> when (val list = articleRepository.all(currentUser.map { it.userId })) {
                is Left -> TODO("allの具体的なエラーが無いため、現在この分岐に入らない想定")
                is Right -> list.value.toSet()
            }
            /**
             * 特定の他ユーザーがお気に入りしているフィルタ 有り
             */
            is Some -> when (
                val otherUser =
                    profileRepository.findByUsername(Username.newWithoutValidation(optionalUserName.value))
            ) {
                /**
                 * ユーザ名に該当する他ユーザーが見つからなかった
                 * - フィルタしても該当する記事は1つも見つからない
                 */
                is Left -> emptySet()
                /**
                 * ユーザ名に該当する他ユーザーが見つかった
                 * - フィルタ済み 作成済み記事一覧
                 */
                is Right -> articleRepository.filterFavoritedByOtherUserId(
                    otherUser.value.userId,
                    currentUser.map { it.userId }
                ).fold(
                    { TODO("filterFavoritedByOtherUserIdのエラーが無いため、現在この分岐に入らない想定") },
                    { it.toSet() }
                )
            }
        }

        /**
         * 3. 作成済み記事にAuthor情報を付与
         * 4. 著者名やタグでフィルタ
         */
        val allFilteredCreatedArticleList = profileRepository.filterByUserIds(
            userIds = filterTargetBase.map { it.authorId }.toSet(),
            viewpointUserId = currentUser.map { it.userId }
        ).fold(
            { TODO("filterByUserIdsのエラーが無いため、この分岐に入ることは想定さていない") },
            /**
             * 3. 作成済み記事にAuthor情報を付与
             */
            { authors ->
                filterTargetBase.map { createdArticle ->
                    authors.find { author -> createdArticle.authorId == author.userId }.toOption().fold(
                        { TODO("必ず見つかる想定なため、この分岐に入ることはない") },
                        { CreatedArticleWithAuthor(article = createdArticle, author = it) }
                    )
                }.toSet()
            }
        ).filter { createdArticleWithAuthor ->
            filterParameters.author.fold(
                /**
                 * 著者名によるフィルタ 無し
                 */
                { true },
                /**
                 * 著者名によるフィルタ 有り
                 */
                { createdArticleWithAuthor.author.username == Username.newWithoutValidation(it) }
            )
        }.filter { createdArticleWithAuthor ->
            filterParameters.tag.fold(
                /**
                 * タグによるフィルタ 無し
                 */
                { true },
                /**
                 * タグによるフィルタ 無し
                 */
                { createdArticleWithAuthor.article.hasTag(Tag.newWithoutValidation(it)) }
            )
        }.toList().sortedBy { it.article.id.value }

        /**
         * 5. LimitとOffsetで返すListを調整
         * - フィルタ後のListのサイズを指定したoffsetが超えていたらエラー
         */
        return if (allFilteredCreatedArticleList.size < filterParameters.offset) {
            FilterCreatedArticleUseCase.Error.OffsetOverCreatedArticlesCountError(
                filterParameters = filterParameters,
                articlesCount = allFilteredCreatedArticleList.size
            ).left()
        } else {
            FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                articles = allFilteredCreatedArticleList
                    .slice(filterParameters.offset until allFilteredCreatedArticleList.size)
                    .take(filterParameters.limit),
                articlesCount = allFilteredCreatedArticleList.size
            ).right()
        }
    }
}
