package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Either
import arrow.core.Option
import arrow.core.none
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface ArticleRepository {
    /**
     * 作成済み記事を全て取得
     *
     * Optional: あるユーザー視点から見た場合
     * - ない場合
     *   - 見つかった作成済み記事は全て 非お気に入り
     * - ある場合
     *   - 見つかった作成済み記事に対して、そのユーザーにとって お気に入り or 非お気に入り の情報がある
     *
     * @param viewpointUserId あるユーザー視点となるユーザーID
     * @return エラー or 作成済み記事の一覧
     */
    fun all(viewpointUserId: Option<UserId> = none()): Either<AllError, List<CreatedArticle>> =
        throw NotImplementedError()

    sealed interface AllError : MyError

    /**
     * 他ユーザーのお気に入りをフィルタ(〇〇さんがお気に入りしている作成済み記事が見たい！)
     *
     * Optional: あるユーザー視点から見た場合
     * - ない場合
     *   - 見つかった作成済み記事は全て 非お気に入り
     * - ある場合
     *   - 見つかった作成済み記事に対して、そのユーザーにとって お気に入り or 非お気に入り の情報がある
     *
     * 注意
     * - 取得できるお気に入り情報はあるユーザー視点からであって、他ユーザーのお気に入り情報ではない
     * 例
     * - otherUserId: AさんのユーザーID
     * - viewpointUserId: BさんのユーザーID
     * - -> 取得した作成済み記事のお気に入り情報は、Bさんから見た場合のお気に入り情報
     *
     * @param otherUserId お気に入りをフィルタしたい他ユーザーID
     * @param viewpointUserId あるユーザー視点となるユーザーID
     * @return 作成済み記事のセット
     */
    fun filterFavoritedByOtherUserId(
        otherUserId: UserId,
        viewpointUserId: Option<UserId> = none()
    ): Either<FilterFavoritedByUserIdError, Set<CreatedArticle>> = throw NotImplementedError()

    sealed interface FilterFavoritedByUserIdError : MyError

    /**
     * Slug で作成された記事検索
     */
    fun findBySlug(slug: Slug): Either<FindBySlugError, CreatedArticle> = throw NotImplementedError()
    sealed interface FindBySlugError : MyError {
        data class NotFound(val slug: Slug) : FindBySlugError, MyError.Basic
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
    fun findBySlugFromRegisteredUserViewpoint(
        slug: Slug,
        userId: UserId
    ): Either<FindBySlugFromRegisteredUserViewpointError, CreatedArticle> = throw NotImplementedError()

    sealed interface FindBySlugFromRegisteredUserViewpointError : MyError {
        data class NotFoundArticle(val slug: Slug) : FindBySlugFromRegisteredUserViewpointError, MyError.Basic
        data class NotFoundUser(val userId: UserId) : FindBySlugFromRegisteredUserViewpointError, MyError.Basic
    }

    /**
     * タグ一覧
     */
    fun tags(): Either<TagsError, List<Tag>> = throw NotImplementedError()
    sealed interface TagsError : MyError

    /**
     * 記事をお気に入りに追加
     *
     * @param slug お気に入りにしたい記事の slug
     * @param currentUserId ログインユーザー
     * @return 準正常系:エラー or 正常系:お気に入り
     */
    fun favorite(slug: Slug, currentUserId: UserId): Either<FavoriteError, CreatedArticle> = throw NotImplementedError()
    sealed interface FavoriteError : MyError {
        data class NotFoundCreatedArticleBySlug(val slug: Slug) : FavoriteError, MyError.Basic
    }

    /**
     * 記事をお気に入りから削除
     *
     * @param slug お気に入りにしたい記事の slug
     * @param currentUserId ログインユーザー
     * @return 準正常系:エラー or 正常系:Unit
     */
    fun unfavorite(slug: Slug, currentUserId: UserId): Either<UnfavoriteError, CreatedArticle> =
        throw NotImplementedError()

    sealed interface UnfavoriteError : MyError {
        data class NotFoundCreatedArticleBySlug(val slug: Slug) : UnfavoriteError, MyError.Basic
    }

    /**
     * 記事を作成
     *
     * @param uncreatedArticle 未作成の記事
     * @return エラー or 作成済みの記事
     */
    fun create(uncreatedArticle: UncreatedArticle): Either<CreateError, CreatedArticle> = throw NotImplementedError()
    sealed interface CreateError : MyError

    /**
     * 作成済み記事を削除
     *   - 紐づくお気に入り関係も削除
     *
     * @param articleId 削除したい作成済み記事のId
     * @return 準正常系:エラー or 正常系:Unit
     */
    fun delete(articleId: ArticleId): Either<DeleteError, Unit> = throw NotImplementedError()
    sealed interface DeleteError : MyError {
        data class NotFoundArticle(val articleId: ArticleId) : DeleteError, MyError.Basic
    }

    /**
     * 作成済み記事を更新
     *
     * @param updatableArticle 削除したい作成済み記事のId
     * @return 準正常系:エラー or 正常系:Unit
     */
    fun update(updatableArticle: UpdatableCreatedArticle): Either<UpdateError, Unit> = throw NotImplementedError()
    sealed interface UpdateError : MyError {
        data class NotFoundArticle(val articleId: ArticleId) : UpdateError, MyError.Basic
    }

    /**
     * 指定した著者郡の最新の作成済み記事郡
     *
     * @param authorIds 著者郡
     * @param viewpointUserId あるユーザー視点となるユーザーID
     * @return 準正常系: エラー or 正常系: 指定した著者の最新の作成済み記事郡
     */
    fun latestByAuthors(
        authorIds: Set<UserId>,
        viewpointUserId: UserId
    ): Either<LatestByAuthorsError, Set<CreatedArticle>> = throw NotImplementedError()

    sealed interface LatestByAuthorsError : MyError
}
