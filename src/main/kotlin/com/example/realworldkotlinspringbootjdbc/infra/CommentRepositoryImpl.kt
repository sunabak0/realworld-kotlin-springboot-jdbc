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

@Repository
class CommentRepositoryImpl(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) : CommentRepository {
    override fun list(slug: Slug): Either<CommentRepository.ListError, List<Comment>> {
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
            return CommentRepository.ListError.Unexpected(e, slug).left()
        }

        /**
         * article が存在しなかった時 NotFoundError
         */
        if (articleFromDb.isEmpty()) {
            return CommentRepository.ListError.NotFoundArticleBySlug(slug).left()
        }
        val articleId = try {
            val it = articleFromDb.first()
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
                , author_id
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
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(it["created_at"].toString()),
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(it["updated_at"].toString()),
                    UserId(it["author_id"].toString().toInt()),
                )
            }.right()
        } catch (e: Throwable) {
            CommentRepository.ListError.Unexpected(e, slug).left()
        }
    }
}
