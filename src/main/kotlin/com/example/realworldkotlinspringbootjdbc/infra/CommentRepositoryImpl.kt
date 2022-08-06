package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.CommentRepository
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

@Repository
class CommentRepositoryImpl(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) : CommentRepository {
    override fun list(slug: Slug): Either<CommentRepository.ListError, List<Comment>> {
        /**
         * article を取得
         */
        val selectArticleSql = """
            SELECT
                id AS id
            FROM
                articles
            WHERE
                articles.slug = :slug
            ;
        """.trimIndent()
        val selectArticleSqlParams = MapSqlParameterSource()
            .addValue("slug", slug.value)
        val articleList = try {
            namedParameterJdbcTemplate.queryForList(selectArticleSql, selectArticleSqlParams)
        } catch (e: Throwable) {
            return CommentRepository.ListError.Unexpected(e, slug).left()
        }

        /**
         * article が存在しなかった時 NotFoundError
         */
        if (articleList.isEmpty()) {
            return CommentRepository.ListError.NotFoundArticleBySlug(slug).left()
        }
        val articleId = try {
            val it = articleList.first()
            ArticleId(it["id"].toString().toInt())
        } catch (e: Throwable) {
            return CommentRepository.ListError.Unexpected(e, slug).left()
        }

        /**
         * comment を取得
         */
        val selectCommentsSql = """
            SELECT
                id AS id
                , body AS body
                , created_at AS created_at
                , updated_at AS updated_at
                , author_id AS author_id
            FROM
                article_comments
            WHERE
                article_comments.article_id = :article_id
            ;
        """.trimIndent()
        val selectCommentsSqlParams = MapSqlParameterSource()
            .addValue("article_id", articleId.value)
        val commentsMap = try {
            namedParameterJdbcTemplate.queryForList(selectCommentsSql, selectCommentsSqlParams)
        } catch (e: Throwable) {
            return CommentRepository.ListError.Unexpected(e, slug).left()
        }

        return try {
            commentsMap.map {
                Comment.newWithoutValidation(
                    CommentId.newWithoutValidation(it["id"].toString().toInt()),
                    Body.newWithoutValidation(it["body"].toString()),
                    createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(it["created_at"].toString()),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(it["updated_at"].toString()),
                    UserId(it["author_id"].toString().toInt()),
                )
            }.right()
        } catch (e: Throwable) {
            CommentRepository.ListError.Unexpected(e, slug).left()
        }
    }

    override fun create(slug: Slug, body: Body, currentUserId: UserId): Either<CommentRepository.CreateError, Comment> {
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
            return CommentRepository.CreateError.Unexpected(e, slug, body, currentUserId).left()
        }

        /**
         * article が存在しなかった時 NotFoundError
         */
        if (articleFromDb.isEmpty()) {
            return CommentRepository.CreateError.NotFoundArticleBySlug(slug).left()
        }
        val articleId = try {
            val it = articleFromDb.first()
            UserId(it["id"].toString().toInt())
        } catch (e: Throwable) {
            return CommentRepository.CreateError.Unexpected(e, slug, body, currentUserId).left()
        }

        /**
         * comment を作成
         */
        val insertCommentSql = """
            INSERT INTO
                article_comments (
                    author_id
                    , article_id
                    , body
                    , created_at
                    , updated_at
                )
            VALUES (
                :author_id
                , :article_id
                , :body
                , :created_at
                , :updated_at
            )
            RETURNING
                id
            ;
        """.trimIndent()
        val now = LocalDateTime.now()
        val insertCommentSqlParams = MapSqlParameterSource()
            .addValue("author_id", currentUserId.value)
            .addValue("article_id", articleId.value)
            .addValue("body", body.value)
            .addValue("created_at", now)
            .addValue("updated_at", now)
        val commentMap = try {
            namedParameterJdbcTemplate.queryForList(insertCommentSql, insertCommentSqlParams)
        } catch (e: Throwable) {
            return CommentRepository.CreateError.Unexpected(e, slug, body, currentUserId).left()
        }

        val commentId = try {
            val it = commentMap.first()
            CommentId.newWithoutValidation(it["id"].toString().toInt())
        } catch (e: Throwable) {
            return CommentRepository.CreateError.Unexpected(e, slug, body, currentUserId).left()
        }

        return try {
            Comment.newWithoutValidation(
                commentId,
                body,
                createdAt = Date.from(now.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()),
                updatedAt = Date.from(now.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()),
                currentUserId
            ).right()
        } catch (e: Throwable) {
            CommentRepository.CreateError.Unexpected(e, slug, body, currentUserId).left()
        }
    }
}
