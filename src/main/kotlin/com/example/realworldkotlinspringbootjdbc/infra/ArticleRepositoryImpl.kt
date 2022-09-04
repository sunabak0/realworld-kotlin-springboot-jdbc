package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.UncreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.text.SimpleDateFormat
import java.util.*
import com.example.realworldkotlinspringbootjdbc.domain.article.Body as ArticleBody

@Repository
class ArticleRepositoryImpl(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) : ArticleRepository {
    override fun all(viewpointUserId: Option<UserId>): Either<ArticleRepository.AllError, List<CreatedArticle>> =
        when (viewpointUserId) {
            /**
             * 作成済み記事一覧
             * - favoritedが常に '0'
             */
            None -> namedParameterJdbcTemplate.queryForList(
                """
                    SELECT
                        articles.id
                        , articles.title
                        , articles.slug
                        , articles.body
                        , articles.created_at
                        , articles.updated_at
                        , articles.description
                        , COALESCE((
                            SELECT
                                STRING_AGG(tags.name, ',')
                            FROM
                                tags
                            JOIN
                                article_tags
                            ON
                                article_tags.tag_id = tags.id
                                AND article_tags.article_id = articles.id
                            GROUP BY
                                article_tags.article_id
                        ), '') AS tags
                        , articles.author_id
                        , '0' AS favorited
                        , (
                            SELECT
                                COUNT(favorites.id)
                            FROM
                                favorites
                            WHERE
                                favorites.article_id = articles.id
                        ) AS favoritesCount
                    FROM
                        articles
                    ;                   
                """.trimIndent(),
                MapSqlParameterSource()
            )
            /**
             * あるユーザー視点から見た作成済み記事一覧
             * - favoritedが'0' or '1'
             */
            is Some -> namedParameterJdbcTemplate.queryForList(
                """
                    SELECT
                        articles.id
                        , articles.title
                        , articles.slug
                        , articles.body
                        , articles.created_at
                        , articles.updated_at
                        , articles.description
                        , COALESCE((
                            SELECT
                                STRING_AGG(tags.name, ',')
                            FROM
                                tags
                            JOIN
                                article_tags
                            ON
                                article_tags.tag_id = tags.id
                                AND article_tags.article_id = articles.id
                            GROUP BY
                                article_tags.article_id
                        ), '') AS tags
                        , articles.author_id
                        , (
                            SELECT
                                CASE COUNT(favorites.id)
                                    WHEN 0 THEN '0'
                                    ELSE '1'
                                END
                            FROM
                                favorites
                            WHERE
                                favorites.article_id = articles.id
                                AND favorites.user_id = :viewpoint_user_id
                        ) AS favorited
                        , (
                            SELECT
                                COUNT(favorites.id)
                            FROM
                                favorites
                            WHERE
                                favorites.article_id = articles.id
                        ) AS favoritesCount
                    FROM
                        articles
                    ;
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("viewpoint_user_id", viewpointUserId.value.value)
            )
        }.map {
            CreatedArticle.newWithoutValidation(
                id = ArticleId(it["id"].toString().toInt()),
                title = Title.newWithoutValidation(it["title"].toString()),
                slug = Slug.newWithoutValidation(it["slug"].toString()),
                body = ArticleBody.newWithoutValidation(it["body"].toString()),
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(it["created_at"].toString()),
                updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(it["updated_at"].toString()),
                description = Description.newWithoutValidation(it["description"].toString()),
                tagList = it["tags"].toString().split(",").map { tag -> Tag.newWithoutValidation(tag) },
                authorId = UserId(it["author_id"].toString().toInt()),
                favorited = it["favorited"].toString() == "1",
                favoritesCount = it["favoritesCount"].toString().toInt()
            )
        }.right()

    override fun filterFavoritedByOtherUserId(
        otherUserId: UserId,
        viewpointUserId: Option<UserId>
    ): Either<ArticleRepository.FilterFavoritedByUserIdError, Set<CreatedArticle>> {
        val sqlParams = MapSqlParameterSource()
            .addValue("other_user_id", otherUserId.value)
        return when (viewpointUserId) {
            /**
             * 他ユーザーのお気に入りである作成済み記事一覧
             * あるユーザー視点 無し
             */
            None -> namedParameterJdbcTemplate.queryForList(
                """
                    SELECT
                        articles.id
                        , articles.title
                        , articles.slug
                        , articles.body
                        , articles.created_at
                        , articles.updated_at
                        , articles.description
                        , COALESCE((
                            SELECT
                                STRING_AGG(tags.name, ',')
                            FROM
                                tags
                            JOIN
                                article_tags
                            ON
                                article_tags.tag_id = tags.id
                                AND article_tags.article_id = articles.id
                            GROUP BY
                                article_tags.article_id
                        ), '') AS tags
                        , articles.author_id
                        , '0' AS favorited
                        , (
                            SELECT
                                COUNT(favorites.id)
                            FROM
                                favorites
                            WHERE
                                favorites.article_id = articles.id
                        ) AS favoritesCount
                    FROM
                        articles
                    JOIN
                        favorites
                    ON
                        favorites.article_id = articles.id
                        AND favorites.user_id = :other_user_id
                    ;
                """.trimIndent(),
                sqlParams
            )
            /**
             * 他ユーザーのお気に入りである作成済み記事一覧
             * あるユーザー視点 有り
             */
            is Some -> namedParameterJdbcTemplate.queryForList(
                """
                    SELECT
                        articles.id
                        , articles.title
                        , articles.slug
                        , articles.body
                        , articles.created_at
                        , articles.updated_at
                        , articles.description
                        , COALESCE((
                            SELECT
                                STRING_AGG(tags.name, ',')
                            FROM
                                tags
                            JOIN
                                article_tags
                            ON
                                article_tags.tag_id = tags.id
                                AND article_tags.article_id = articles.id
                            GROUP BY
                                article_tags.article_id
                        ), '') AS tags
                        , articles.author_id
                        , (
                            SELECT
                                CASE COUNT(favorites.id)
                                    WHEN 0 THEN '0'
                                    ELSE '1'
                                END
                            FROM
                                favorites
                            WHERE
                                favorites.article_id = articles.id
                                AND favorites.user_id = :viewpoint_user_id
                        ) AS favorited
                        , (
                            SELECT
                                COUNT(favorites.id)
                            FROM
                                favorites
                            WHERE
                                favorites.article_id = articles.id
                        ) AS favoritesCount
                    FROM
                        articles
                    JOIN
                        favorites
                    ON
                        favorites.article_id = articles.id
                        AND favorites.user_id = :other_user_id
                    ;
                """.trimIndent(),
                sqlParams
                    .addValue("viewpoint_user_id", viewpointUserId.value.value)
            )
        }.map {
            CreatedArticle.newWithoutValidation(
                id = ArticleId(it["id"].toString().toInt()),
                title = Title.newWithoutValidation(it["title"].toString()),
                slug = Slug.newWithoutValidation(it["slug"].toString()),
                body = ArticleBody.newWithoutValidation(it["body"].toString()),
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(it["created_at"].toString()),
                updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(it["updated_at"].toString()),
                description = Description.newWithoutValidation(it["description"].toString()),
                tagList = it["tags"].toString().split(",").map { tag -> Tag.newWithoutValidation(tag) },
                authorId = UserId(it["author_id"].toString().toInt()),
                favorited = it["favorited"].toString() == "1",
                favoritesCount = it["favoritesCount"].toString().toInt()
            )
        }.toSet().right()
    }

    override fun findBySlug(slug: Slug): Either<ArticleRepository.FindBySlugError, CreatedArticle> {
        val selectBySlugSql = """
            SELECT
                articles.id
                , articles.title
                , articles.slug
                , articles.body
                , articles.created_at
                , articles.updated_at
                , articles.description
                , COALESCE((
                    SELECT
                        STRING_AGG(tags.name, ',')
                    FROM
                        tags
                    JOIN
                        article_tags
                    ON
                        article_tags.tag_id = tags.id
                        AND article_tags.article_id = articles.id
                    GROUP BY
                        article_tags.article_id
                ), '') AS tags
                , articles.author_id
                , (
                    SELECT
                        COUNT(favorites.id)
                    FROM
                        favorites
                    WHERE
                        favorites.article_id = articles.id
                ) AS favoritesCount
            FROM
                articles
            WHERE
                articles.slug = :slug
            ;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource()
            .addValue("slug", slug.value)
        val articleList = try {
            namedParameterJdbcTemplate.queryForList(selectBySlugSql, sqlParams)
        } catch (e: Throwable) {
            return ArticleRepository.FindBySlugError.Unexpected(e, slug).left()
        }
        return when {
            articleList.isEmpty() -> ArticleRepository.FindBySlugError.NotFound(slug).left()
            else -> try {
                val articleMap = articleList.first()
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(articleMap["id"].toString().toInt()),
                    title = Title.newWithoutValidation(articleMap["title"].toString()),
                    slug = Slug.newWithoutValidation(articleMap["slug"].toString()),
                    body = ArticleBody.newWithoutValidation(articleMap["body"].toString()),
                    createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(articleMap["created_at"].toString()),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(articleMap["updated_at"].toString()),
                    description = Description.newWithoutValidation(articleMap["description"].toString()),
                    tagList = articleMap["tags"].toString().split(",").map { Tag.newWithoutValidation(it) },
                    authorId = UserId(articleMap["author_id"].toString().toInt()),
                    favorited = false,
                    favoritesCount = articleMap["favoritesCount"].toString().toInt()
                ).right()
            } catch (e: Throwable) {
                ArticleRepository.FindBySlugError.Unexpected(e, slug).left()
            }
        }
    }

    override fun findBySlugFromRegisteredUserViewpoint(
        slug: Slug,
        userId: UserId
    ): Either<ArticleRepository.FindBySlugFromRegisteredUserViewpointError, CreatedArticle> {
        val selectUserExistedSql = """
            SELECT
                id
            FROM
                users
            WHERE
                id = :user_id
            ;
        """.trimIndent()
        val selectUserExistedSqlParams = MapSqlParameterSource()
            .addValue("user_id", userId.value)
        val userList = try {
            namedParameterJdbcTemplate.queryForList(selectUserExistedSql, selectUserExistedSqlParams)
        } catch (e: Throwable) {
            return ArticleRepository.FindBySlugFromRegisteredUserViewpointError.Unexpected(e, slug).left()
        }
        if (userList.isEmpty()) {
            return ArticleRepository.FindBySlugFromRegisteredUserViewpointError.NotFoundUser(userId).left()
        }
        val selectBySlugSql = """
            SELECT
                articles.id
                , articles.title
                , articles.slug
                , articles.body
                , articles.created_at
                , articles.updated_at
                , articles.description
                , COALESCE((
                    SELECT
                        STRING_AGG(tags.name, ',')
                    FROM
                        tags
                    JOIN
                        article_tags
                    ON
                        article_tags.tag_id = tags.id
                        AND article_tags.article_id = articles.id
                    GROUP BY
                        article_tags.article_id
                ), '') AS tags
                , articles.author_id
                , (
                    SELECT
                        CASE COUNT(favorites.id)
                            WHEN 0 THEN 0
                            ELSE 1
                        END
                    FROM
                        favorites
                    WHERE
                        favorites.article_id = articles.id
                        AND favorites.user_id = :user_id
                ) AS favorited
                , (
                    SELECT
                        COUNT(favorites.id)
                    FROM
                        favorites
                    WHERE
                        favorites.article_id = articles.id
                ) AS favoritesCount
            FROM
                articles
            WHERE
                articles.slug = :slug
            ;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource()
            .addValue("slug", slug.value)
            .addValue("user_id", userId.value)
        val articleList = try {
            namedParameterJdbcTemplate.queryForList(selectBySlugSql, sqlParams)
        } catch (e: Throwable) {
            return ArticleRepository.FindBySlugFromRegisteredUserViewpointError.Unexpected(e, slug).left()
        }
        return when {
            articleList.isEmpty() -> ArticleRepository.FindBySlugFromRegisteredUserViewpointError.NotFoundArticle(slug)
                .left()

            else -> {
                val articleMap = articleList.first()

                try {
                    CreatedArticle.newWithoutValidation(
                        id = ArticleId(articleMap["id"].toString().toInt()),
                        title = Title.newWithoutValidation(articleMap["title"].toString()),
                        slug = Slug.newWithoutValidation(articleMap["slug"].toString()),
                        body = ArticleBody.newWithoutValidation(articleMap["body"].toString()),
                        createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(articleMap["created_at"].toString()),
                        updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(articleMap["updated_at"].toString()),
                        description = Description.newWithoutValidation(articleMap["description"].toString()),
                        tagList = articleMap["tags"].toString().split(",").map { Tag.newWithoutValidation(it) },
                        authorId = UserId(articleMap["author_id"].toString().toInt()),
                        favorited = articleMap["favorited"].toString() == "1",
                        favoritesCount = articleMap["favoritesCount"].toString().toInt()
                    ).right()
                } catch (e: Throwable) {
                    ArticleRepository.FindBySlugFromRegisteredUserViewpointError.Unexpected(e, slug).left()
                }
            }
        }
    }

    override fun tags(): Either<ArticleRepository.TagsError, List<Tag>> {
        val tagListSql = """
            SELECT
                name
            FROM
                tags
            ;
        """.trimIndent()
        return try {
            namedParameterJdbcTemplate.queryForList(tagListSql, MapSqlParameterSource())
                .map { Tag.newWithoutValidation(it["name"].toString()) }
                .right()
        } catch (e: Throwable) {
            ArticleRepository.TagsError.Unexpected(e).left()
        }
    }

    override fun favorite(slug: Slug, currentUserId: UserId): Either<ArticleRepository.FavoriteError, CreatedArticle> {
        /**
         * article を取得
         */
        val selectArticleSql = """
            SELECT
                id
            FROM
                articles
            WHERE
                slug = :slug
        """.trimIndent()
        val selectArticleSqlParams = MapSqlParameterSource()
            .addValue("slug", slug.value)
        val articleFromDb = try {
            namedParameterJdbcTemplate.queryForList(selectArticleSql, selectArticleSqlParams)
        } catch (e: Throwable) {
            return ArticleRepository.FavoriteError.Unexpected(e, slug, currentUserId).left()
        }

        /**
         * article が存在しなかったとき NotFoundError
         */
        if (articleFromDb.isEmpty()) {
            return ArticleRepository.FavoriteError.NotFoundCreatedArticleBySlug(slug).left()
        }
        val articleId = try {
            val it = articleFromDb.first()
            ArticleId(it["id"].toString().toInt())
        } catch (e: Throwable) {
            return ArticleRepository.FavoriteError.Unexpected(e, slug, currentUserId).left()
        }

        /**
         * お気に入りではないとき、お気に入りに追加する
         */
        val insertFavoritesSql = """
            INSERT INTO favorites
                (
                    user_id
                    , article_id
                    , created_at
                )
            SELECT
                user_id
                , article_id
                , created_at
            FROM
                (
                    SELECT
                        :user_id AS user_id
                        , :article_id AS article_id
                        , NOW() AS created_at
                ) AS tmp
            WHERE
                NOT EXISTS (
                    SELECT
                        1
                    FROM
                        favorites
                    WHERE
                        user_id = :user_id
                        AND article_id = :article_id
                )
            ;
        """.trimIndent()
        val insertFavoritesSqlParams = MapSqlParameterSource()
            .addValue("user_id", currentUserId.value)
            .addValue("article_id", articleId.value)
        try {
            namedParameterJdbcTemplate.update(insertFavoritesSql, insertFavoritesSqlParams)
        } catch (e: Throwable) {
            return ArticleRepository.FavoriteError.Unexpected(e, slug, currentUserId).left()
        }

        /**
         * Slug に該当する、作成済記事を取得する
         */
        val selectBySlugSql = """
            SELECT
                articles.id
                , articles.title
                , articles.slug
                , articles.body
                , articles.created_at
                , articles.updated_at
                , articles.description
                , COALESCE((
                    SELECT
                        STRING_AGG(tags.name, ',')
                    FROM
                        tags
                    JOIN
                        article_tags
                    ON
                        article_tags.tag_id = tags.id
                        AND article_tags.article_id = articles.id
                    GROUP BY
                        article_tags.article_id
                ), '') AS tags
                , articles.author_id
                , (
                    SELECT
                        CASE COUNT(favorites.id)
                            WHEN 0 THEN 0
                            ELSE 1
                        END
                    FROM
                        favorites
                    WHERE
                        favorites.article_id = articles.id
                        AND favorites.user_id = :user_id
                ) AS favorited
                , (
                    SELECT
                        COUNT(favorites.id)
                    FROM
                        favorites
                    WHERE
                        favorites.article_id = articles.id
                ) AS favoritesCount
            FROM
                articles
            WHERE
                articles.slug = :slug
            ;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource()
            .addValue("slug", slug.value)
            .addValue("user_id", currentUserId.value)
        val articleMap = namedParameterJdbcTemplate.queryForList(selectBySlugSql, sqlParams).first()
        return try {
            CreatedArticle.newWithoutValidation(
                id = ArticleId(articleMap["id"].toString().toInt()),
                title = Title.newWithoutValidation(articleMap["title"].toString()),
                slug = Slug.newWithoutValidation(articleMap["slug"].toString()),
                body = com.example.realworldkotlinspringbootjdbc.domain.article.Body.newWithoutValidation(articleMap["body"].toString()),
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(articleMap["created_at"].toString()),
                updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(articleMap["updated_at"].toString()),
                description = Description.newWithoutValidation(articleMap["description"].toString()),
                tagList = articleMap["tags"].toString().split(",").map { Tag.newWithoutValidation(it) },
                authorId = UserId(articleMap["author_id"].toString().toInt()),
                favorited = articleMap["favorited"].toString() == "1",
                favoritesCount = articleMap["favoritesCount"].toString().toInt()
            ).right()
        } catch (e: Throwable) {
            ArticleRepository.FavoriteError.Unexpected(e, slug, currentUserId).left()
        }
    }

    override fun unfavorite(
        slug: Slug,
        currentUserId: UserId
    ): Either<ArticleRepository.UnfavoriteError, CreatedArticle> {
        /**
         * article を取得
         */
        val selectArticleSql = """
            SELECT
                id
            FROM
                articles
            WHERE
                slug = :slug
        """.trimIndent()
        val selectArticleSqlParams = MapSqlParameterSource()
            .addValue("slug", slug.value)
        val articleFromDb = try {
            namedParameterJdbcTemplate.queryForList(selectArticleSql, selectArticleSqlParams)
        } catch (e: Throwable) {
            return ArticleRepository.UnfavoriteError.Unexpected(e, slug, currentUserId).left()
        }

        /**
         * article が存在しなかったとき NotFoundError
         */
        if (articleFromDb.isEmpty()) {
            return ArticleRepository.UnfavoriteError.NotFoundCreatedArticleBySlug(slug).left()
        }
        val articleId = try {
            val it = articleFromDb.first()
            ArticleId(it["id"].toString().toInt())
        } catch (e: Throwable) {
            return ArticleRepository.UnfavoriteError.Unexpected(e, slug, currentUserId).left()
        }

        /**
         * お気に入りのとき、お気に入りから削除する
         */
        val deleteFavoritesSql = """
            DELETE FROM
                favorites
            WHERE
                favorites.article_id = :article_id
                AND favorites.user_id = :user_id
            ;
        """.trimIndent()
        val deleteFavoritesSqlParams = MapSqlParameterSource()
            .addValue("article_id", articleId.value)
            .addValue("user_id", currentUserId.value)
        try {
            namedParameterJdbcTemplate.update(deleteFavoritesSql, deleteFavoritesSqlParams)
        } catch (e: Throwable) {
            return ArticleRepository.UnfavoriteError.Unexpected(e, slug, currentUserId).left()
        }

        /**
         * Slug に該当する、作成済記事を取得する
         */
        val selectBySlugSql = """
            SELECT
                articles.id
                , articles.title
                , articles.slug
                , articles.body
                , articles.created_at
                , articles.updated_at
                , articles.description
                , COALESCE((
                    SELECT
                        STRING_AGG(tags.name, ',')
                    FROM
                        tags
                    JOIN
                        article_tags
                    ON
                        article_tags.tag_id = tags.id
                        AND article_tags.article_id = articles.id
                    GROUP BY
                        article_tags.article_id
                ), '') AS tags
                , articles.author_id
                , (
                    SELECT
                        CASE COUNT(favorites.id)
                            WHEN 0 THEN 0
                            ELSE 1
                        END
                    FROM
                        favorites
                    WHERE
                        favorites.article_id = articles.id
                        AND favorites.user_id = :user_id
                ) AS favorited
                , (
                    SELECT
                        COUNT(favorites.id)
                    FROM
                        favorites
                    WHERE
                        favorites.article_id = articles.id
                ) AS favoritesCount
            FROM
                articles
            WHERE
                articles.slug = :slug
            ;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource()
            .addValue("slug", slug.value)
            .addValue("user_id", currentUserId.value)
        val articleMap = namedParameterJdbcTemplate.queryForList(selectBySlugSql, sqlParams).first()
        return try {
            CreatedArticle.newWithoutValidation(
                id = ArticleId(articleMap["id"].toString().toInt()),
                title = Title.newWithoutValidation(articleMap["title"].toString()),
                slug = Slug.newWithoutValidation(articleMap["slug"].toString()),
                body = com.example.realworldkotlinspringbootjdbc.domain.article.Body.newWithoutValidation(articleMap["body"].toString()),
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(articleMap["created_at"].toString()),
                updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(articleMap["updated_at"].toString()),
                description = Description.newWithoutValidation(articleMap["description"].toString()),
                tagList = articleMap["tags"].toString().split(",").map { Tag.newWithoutValidation(it) },
                authorId = UserId(articleMap["author_id"].toString().toInt()),
                favorited = articleMap["favorited"].toString() == "1",
                favoritesCount = articleMap["favoritesCount"].toString().toInt()
            ).right()
        } catch (e: Throwable) {
            ArticleRepository.UnfavoriteError.Unexpected(e, slug, currentUserId).left()
        }
    }

    @Transactional
    override fun create(uncreatedArticle: UncreatedArticle): Either<ArticleRepository.CreateError, CreatedArticle> {
        val currentDate = Date()
        val insertArticleSql = """
            INSERT INTO articles
                (
                    author_id
                    , slug
                    , title
                    , body
                    , description
                    , created_at
                    , updated_at
                )
            VALUES
                (
                    :author_id
                    , :slug
                    , :title
                    , :body
                    , :description
                    , :created_at
                    , :updated_at
                )
            RETURNING
                id
            ;
        """.trimIndent()
        val articleId = namedParameterJdbcTemplate.queryForMap(
            insertArticleSql,
            MapSqlParameterSource()
                .addValue("author_id", uncreatedArticle.authorId.value)
                .addValue("slug", uncreatedArticle.slug.value)
                .addValue("title", uncreatedArticle.title.value)
                .addValue("description", uncreatedArticle.description.value)
                .addValue("body", uncreatedArticle.body.value)
                .addValue("created_at", currentDate)
                .addValue("updated_at", currentDate)
        )["id"].toString().toInt()

        val createdArticle = CreatedArticle.newWithoutValidation(
            id = ArticleId(articleId),
            title = uncreatedArticle.title,
            slug = uncreatedArticle.slug,
            body = uncreatedArticle.body,
            description = uncreatedArticle.description,
            tagList = uncreatedArticle.tagList,
            authorId = uncreatedArticle.authorId,
            favorited = false,
            favoritesCount = 0,
            createdAt = currentDate,
            updatedAt = currentDate,
        )

        /**
         * タグリストがない -> 不要-永続化
         */
        if (uncreatedArticle.tagList.isEmpty()) {
            return createdArticle.right()
        }

        /**
         * タグリストがある -> 要-永続化
         */
        val bulkInsertTagsSql = """
            INSERT INTO tags
                (
                    name
                    , created_at
                    , updated_at
                )
            VALUES
                (
                    :name
                    , :created_at
                    , :updated_at
                )
            ON CONFLICT
                (
                    name
                )
            DO NOTHING
            ;
        """.trimIndent()
        namedParameterJdbcTemplate.batchUpdate(
            bulkInsertTagsSql,
            uncreatedArticle.tagList.map {
                MapSqlParameterSource()
                    .addValue("name", it.value)
                    .addValue("created_at", currentDate)
                    .addValue("updated_at", currentDate)
            }.toTypedArray()
        )
        val selectTagsSql = """
            SELECT
                id
            FROM
                tags
            WHERE
                name
            IN (:name)
            ;
        """.trimIndent()
        val tagIdList = namedParameterJdbcTemplate.queryForList(
            selectTagsSql,
            MapSqlParameterSource().addValue("name", uncreatedArticle.tagList.map { it.value }.toSet())
        ).map { it["id"].toString().toInt() }
        val bulkInsertRelationsOfArticleToTagSql = """
            INSERT INTO article_tags
                (
                    article_id
                    , tag_id
                    , created_at
                )
            VALUES
                (
                    :article_id
                    , :tag_id
                    , :created_at
                )
            ;
        """.trimIndent()
        namedParameterJdbcTemplate.batchUpdate(
            bulkInsertRelationsOfArticleToTagSql,
            tagIdList.map { tagId ->
                MapSqlParameterSource()
                    .addValue("article_id", articleId)
                    .addValue("tag_id", tagId)
                    .addValue("created_at", currentDate)
            }.toTypedArray()
        )
        return createdArticle.right()
    }
}
