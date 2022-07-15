package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.left
import arrow.core.right
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
    override fun list(slug: Slug, currentUserId: UserId): Either<CommentRepository.ListError, List<Comment>> {
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
            UserId(it["id"].toString().toInt())
        } catch (e: Throwable) {
            return CommentRepository.ListError.Unexpected(e, slug).left()
        }

        /**
         * comment を取得
         */
        val selectCommentsSql = """
            SELECT
                article_comments.id AS id
                , article_comments.body AS body
                , article_comments.created_at AS created_at
                , article_comments.updated_at AS updated_at
                , comment_author_profile.user_id AS user_id
                , comment_author_profile.username AS username
                , comment_author_profile.bio AS bio
                , comment_author_profile.image AS image
                , comment_author_profile.following_flg AS following_flg
            FROM
                article_comments
            JOIN (
                SELECT
                    users.id AS user_id
                    , users.username AS username
                    , profiles.bio AS bio
                    , profiles.image AS image
                    , CASE WHEN followings.id IS NOT NULL THEN 1 ELSE 0 END AS following_flg
                FROM
                    users
                JOIN
                    profiles
                ON
                    users.id = profiles.user_id
                    AND users.id = article_comments.author_id 
                LEFT OUTER JOIN
                    followings
                ON
                    followings.following_id = users.id
                    AND followings.follower_id = :current_user_id
            ) AS comment_author_profile
            ON
                article_comments.author_id = comment_author_profile.user_id
                AND article_comments.article_id = :article_id
        """.trimIndent()
        val selectCommentsSqlParams = MapSqlParameterSource()
            .addValue("current_user_id", currentUserId.value)
            .addValue("article_id", articleId.value)
        val commentsFromDb = try {
            namedParameterJdbcTemplate.queryForList(selectCommentsSql, selectCommentsSqlParams)
        } catch (e: Throwable) {
            return CommentRepository.ListError.Unexpected(e, slug).left()
        }

        return try {
            commentsFromDb.map {
                Comment.newWithoutValidation(
                    CommentId.newWithoutValidation(it["id"].toString().toInt()),
                    Body.newWithoutValidation(it["body"].toString()),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse(it["created_at"].toString()),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse(it["updated_at"].toString()),
                    UserId(it["user_id"].toString().toInt()),
                )
            }.right()
        } catch (e: Throwable) {
            CommentRepository.ListError.Unexpected(e, slug).left()
        }
    }
}
