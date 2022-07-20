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
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.text.SimpleDateFormat
import com.example.realworldkotlinspringbootjdbc.domain.article.Body as ArticleBody

@Repository
class ArticleRepositoryImpl(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) : ArticleRepository {
    private val selectBySlugSql = """
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
                        WHEN 0 THEN false
                        ELSE true
                    END
                FROM
                    favorites
                WHERE
                    favorites.article_id = articles.id
                    AND favorites.user_id = :current_user_id
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
    override fun findBySlug(slug: Slug): Either<ArticleRepository.FindBySlugError, CreatedArticle> {
        val selectArticleSqlParams = MapSqlParameterSource()
            .addValue("slug", slug.value)
            .addValue("current_user_id", null)
        val articleList = try {
            namedParameterJdbcTemplate.queryForList(selectBySlugSql, selectArticleSqlParams)
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
                    favorited = articleMap["favorited"].toString() == "true",
                    favoritesCount = articleMap["favoritesCount"].toString().toInt()
                ).right()
            } catch (e: Throwable) {
                ArticleRepository.FindBySlugError.Unexpected(e, slug).left()
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
}
