package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
<<<<<<< HEAD
=======
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
>>>>>>> 2f51988 (feat: 「slug から単一記事取得」機能追加 のため、ArticleRepository.findBySlug() を追加)
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.text.SimpleDateFormat
import com.example.realworldkotlinspringbootjdbc.domain.article.Body as ArticleBody

@Repository
class ArticleRepositoryImpl(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) : ArticleRepository {
    override fun findBySlug(slug: Slug): Either<ArticleRepository.FindBySlugError, CreatedArticle> {
        /**
         * slug に該当する 記事 を取得
         */
        val selectArticleSql = """
            SELECT
                articles.id
                , articles.title
                , articles.slug
                , articles.body
                , articles.created_at
                , articles.updated_at
                , articles.description
                , articles.author_id
                , 0 AS favorited
                , (
                    SELECT
                        COUNT(favorites.user_id)
                    FROM
                        favorites
                    WHERE
                        favorites.article_id = articles.id
                    ) AS favoritesCount
            FROM
                articles
            WHERE
                slug = :slug
            ;
        """.trimIndent()
        val selectArticleSqlParams = MapSqlParameterSource()
            .addValue("slug", slug.value)
        val articleMap = try {
            namedParameterJdbcTemplate.queryForList(selectArticleSql, selectArticleSqlParams)
        } catch (e: Throwable) {
            return ArticleRepository.FindBySlugError.Unexpected(e, slug).left()
        }

        /**
         * tag 取得のため、articleId だけ取得
         */
        val articleId = when (articleMap.isEmpty()) {
            /**
             * article が存在しなかった場合、NotFoundError
             */
            true -> return ArticleRepository.FindBySlugError.NotFound(slug).left()
            /**
             * article が存在した場合、id を取得
             */
            false -> try {
                ArticleId(articleMap.first()["id"].toString().toInt())
            } catch (e: Throwable) {
                return ArticleRepository.FindBySlugError.Unexpected(e, slug).left()
            }
        }

        /**
         * article に該当する tag を取得
         */
        val selectTagsSql = """
            SELECT
                tags.name AS name
            FROM
                article_tags
            JOIN
                tags
            ON
                article_tags.tag_id = tags.id
            WHERE
                article_tags.article_id = :article_id
            ;
        """.trimIndent()
        val selectTagsSqlParams = MapSqlParameterSource()
            .addValue("article_id", articleId.value)
        val tagsMap = try {
            namedParameterJdbcTemplate.queryForList(selectTagsSql, selectTagsSqlParams)
        } catch (e: Throwable) {
            return ArticleRepository.FindBySlugError.Unexpected(e, slug).left()
        }
        val tags = try {
            tagsMap.map { Tag.newWithoutValidation(it["name"].toString()) }
        } catch (e: Throwable) {
            return ArticleRepository.FindBySlugError.Unexpected(e, slug).left()
        }

        return try {
            CreatedArticle.newWithoutValidation(
                articleId,
                Title.newWithoutValidation(articleMap.first()["title"].toString()),
                Slug.newWithoutValidation(articleMap.first()["slug"].toString()),
                ArticleBody.newWithoutValidation(articleMap.first()["body"].toString()),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(articleMap.first()["created_at"].toString()),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(articleMap.first()["updated_at"].toString()),
                Description.newWithoutValidation(articleMap.first()["description"].toString()),
                tags,
                UserId(articleMap.first()["author_id"].toString().toInt()),
                favorited = articleMap.first()["favorited"].toString().toInt() == 1,
                favoritesCount = articleMap.first()["favoritesCount"].toString().toInt()
            ).right()
        } catch (e: Throwable) {
            return ArticleRepository.FindBySlugError.Unexpected(e, slug).left()
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
}
