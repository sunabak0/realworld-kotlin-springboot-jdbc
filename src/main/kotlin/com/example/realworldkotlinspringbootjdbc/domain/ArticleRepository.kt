package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.article.FilterParameters
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.util.MyError
/**
 * 作成済み記事リスト with Metadata
 *
 * first: 作成済み記事リスト
 * second: フィルタにかかった記事の総数
 */
typealias CreatedArticlesWithMetadata = Pair<List<CreatedArticle>, Int>

interface ArticleRepository {
    /**
     * Slug で作成された記事検索
     */
    fun findBySlug(slug: Slug): Either<FindBySlugError, CreatedArticle> = TODO()
    sealed interface FindBySlugError : MyError {
        data class NotFound(val slug: Slug) : FindBySlugError, MyError.Basic
        data class Unexpected(override val cause: Throwable, val slug: Slug) : FindBySlugError, MyError.MyErrorWithThrowable
    }

    /**
     * 特定の登録済みユーザーから見た作成済み記事 検索 by Slug
     *
     * 備考
     * 特定の登録済ユーザーから見て、見つかった作成済み記事に対して お気に入り/非お気に入り の有無がある
     *
     * @param slug 検索したいSlug
     * @param userId 登録済みユーザーid
     * @return 準正常系:エラー or 正常系:作成済み記事
     */
    fun findBySlugFromRegisteredUserViewpoint(slug: Slug, userId: UserId): Either<FindBySlugFromRegisteredUserViewpointError, CreatedArticle> = TODO()
    sealed interface FindBySlugFromRegisteredUserViewpointError : MyError {
        data class NotFoundArticle(val slug: Slug) : FindBySlugFromRegisteredUserViewpointError, MyError.Basic
        data class NotFoundUser(val userId: UserId) : FindBySlugFromRegisteredUserViewpointError, MyError.Basic
        data class Unexpected(override val cause: Throwable, val slug: Slug) : FindBySlugFromRegisteredUserViewpointError, MyError.MyErrorWithThrowable
    }

    /**
     * タグ一覧
     */
    fun tags(): Either<TagsError, List<Tag>> = TODO("テストでDIする時、余計なoverrideを記述不要にするため")
    sealed interface TagsError : MyError {
        data class Unexpected(override val cause: Throwable) : TagsError, MyError.MyErrorWithThrowable
    }

    /**
     * 記事をお気に入りに追加
     *
     * @param slug お気に入りにしたい記事の slug
     * @param currentUserId ログインユーザー
     * @return 準正常系:エラー or 正常系:お気に入り
     */
    fun favorite(slug: Slug, currentUserId: UserId): Either<FavoriteError, CreatedArticle> = TODO()
    sealed interface FavoriteError : MyError {
        data class NotFoundCreatedArticleBySlug(val slug: Slug) : FavoriteError, MyError.Basic
        data class Unexpected(override val cause: Throwable, val slug: Slug, val currentUserId: UserId) :
            FavoriteError,
            MyError.MyErrorWithThrowable
    }

    /**
     * 記事をお気に入りから削除
     *
     * @param slug お気に入りにしたい記事の slug
     * @param currentUserId ログインユーザー
     * @return 準正常系:エラー or 正常系:Unit
     */
    fun unfavorite(slug: Slug, currentUserId: UserId): Either<UnfavoriteError, CreatedArticle> = TODO()
    sealed interface UnfavoriteError : MyError {
        data class NotFoundCreatedArticleBySlug(val slug: Slug) : UnfavoriteError, MyError.Basic
        data class Unexpected(override val cause: Throwable, val slug: Slug, val currentUserId: UserId) :
            UnfavoriteError,
            MyError.MyErrorWithThrowable
    }

    /**
     * 作成済み記事をフィルタ(フィルタがなければただの一覧取得)
     *
     * @param filterParameters フィルタ用パラメーター
     * @return エラー or フィルタされた作成済み記事の一覧とフィルタにかかった記事の総数
     */
    fun filter(filterParameters: FilterParameters): Either<FilterError, CreatedArticlesWithMetadata> = TODO()
    sealed interface FilterError : MyError {
        data class OffsetOverTheNumberOfFilteredCreatedArticlesError(
            val filterParameters: FilterParameters
        ) : FilterError, MyError.Basic
    }

    /**
     * 特定のユーザーから見た作成済み記事をフィルタ(フィルタがなければただの一覧取得)
     *
     * 備考
     * - 特定の登録済ユーザーから見て、見つかった作成済み記事に対して お気に入り/非お気に入り の有無がある
     * - 特定の登録済ユーザーから見て、見つかった作成済み記事の著者に対して フォロー/未フォロー の有無がある
     *
     * @param filterParameters フィルタ用パラメーター
     * @param userId ユーザーID
     * @return エラー or フィルタされた作成済み記事の一覧とフィルタにかかった記事の総数
     */
    fun filterFromRegisteredUserViewpoint(filterParameters: FilterParameters, userId: UserId): Either<FilterError, CreatedArticlesWithMetadata> = TODO()
    sealed interface FilterFromRegisteredUserViewpointError : MyError {
        data class OffsetOverTheNumberOfFilteredCreatedArticlesError(
            val filterParameters: FilterParameters
        ) : FilterFromRegisteredUserViewpointError, MyError.Basic
        data class NotFoundUser(val userId: UserId) : FilterFromRegisteredUserViewpointError, MyError.Basic
    }
}
