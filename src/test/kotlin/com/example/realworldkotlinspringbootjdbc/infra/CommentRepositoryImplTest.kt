package com.example.realworldkotlinspringbootjdbc.infra

import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import java.text.SimpleDateFormat

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("WithLocalDb")
class CommentRepositoryImplTest {
    companion object {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
        fun resetDb() {
            val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
            val sql = """
                DELETE FROM users;
                DELETE FROM profiles;
                DELETE FROM followings;
                DELETE FROM articles;
            """.trimIndent()
            namedParameterJdbcTemplate.update(sql, MapSqlParameterSource())
        }
    }

    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class `List(コメント一覧を表示)` {
        @BeforeEach
        @AfterAll
        fun reset() {
            resetDb()
        }

        @Test
        fun `正常系、Comment の List が戻り値`() {
            fun localPrepare() {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")

                val insertArticleSql = """
                    INSERT INTO
                        articles (
                            id
                            , author_id
                            , title
                            , slug
                            , body
                            , description
                            , created_at
                            , updated_at
                        )
                    VALUES (
                        :id
                        , :author_id
                        , :title
                        , :slug
                        , :body
                        , :description
                        , :created_at
                        , :updated_at
                    );
                """.trimIndent()
                val insertArticleSqlParams = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("author_id", 1)
                    .addValue("title", "dummy-title")
                    .addValue("slug", "dummy-slug")
                    .addValue("body", "dummy-body")
                    .addValue("description", "dummy-description")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertArticleSql, insertArticleSqlParams)

                val insertUserSql =
                    "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
                val insertUserSqlParams = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("email", "dummy@example.com")
                    .addValue("username", "dummy-username")
                    .addValue("password", "Passw0rd")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertUserSql, insertUserSqlParams)

                val insertProfileSql =
                    "INSERT INTO profiles(id, user_id, bio, image, created_at, updated_at) VALUES (:id, :user_id, :bio, :image, :created_at, :updated_at);"
                val insertProfileSqlParams1 = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("user_id", 1)
                    .addValue("bio", "dummy-bio")
                    .addValue("image", "dummy-image")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertProfileSql, insertProfileSqlParams1)
                val insertProfileSqlParams2 = MapSqlParameterSource()
                    .addValue("id", 2)
                    .addValue("user_id", 2)
                    .addValue("bio", "dummy-bio")
                    .addValue("image", "dummy-image")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertProfileSql, insertProfileSqlParams2)

                val insertFollowingsSql =
                    "INSERT INTO followings(id, following_id, follower_id, created_at) VALUES (:id, :following_id, :follower_id, :created_at);"
                val insertFollowingsSqlParams = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("following_id", 1)
                    .addValue("follower_id", 2)
                    .addValue("created_at", date)
                namedParameterJdbcTemplate.update(insertFollowingsSql, insertFollowingsSqlParams)
            }
            localPrepare()

            val commentRepository = CommentRepositoryImpl(namedParameterJdbcTemplate)

            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val actual = commentRepository.list(Slug.newWithoutValidation("dummy-slug"), UserId(1))
            val expected = listOf(
                Comment.newWithoutValidation(
                    CommentId.newWithoutValidation(1),
                    Body.newWithoutValidation("dummy-1"),
                    date,
                    date,
                    UserId(1),
                ),
                Comment.newWithoutValidation(
                    CommentId.newWithoutValidation(2),
                    Body.newWithoutValidation("dummy-2"),
                    date,
                    date,
                    UserId(2),
                ),
            )
            assertThat(actual).isEqualTo(expected)
        }
    }
}
