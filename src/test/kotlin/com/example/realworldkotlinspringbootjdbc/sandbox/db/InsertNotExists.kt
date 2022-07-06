package com.example.realworldkotlinspringbootjdbc.sandbox.db

import com.example.realworldkotlinspringbootjdbc.infra.DbConnection
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import java.text.SimpleDateFormat

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("WithLocalDb")
class InsertNotExists {
    @BeforeEach
    @AfterEach
    fun resetDb() {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
        val sql1 = """
            DELETE FROM followings;
            DELETE FROM users;
        """.trimIndent()
        namedParameterJdbcTemplate.update(sql1, MapSqlParameterSource())
    }

    private val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate

    @Test
    fun `レコードが存在しないとき、INSERT する`() {
        val sql = """
            INSERT INTO followings
                (
                    following_id
                    , follower_id 
                    , created_at
                )
            SELECT
                :user_id
                , :current_user_id
                , NOW()
            WHERE
                NOT EXISTS (
                    SELECT
                        follower_id
                    FROM
                        followings
                    JOIN
                        users
                    ON
                        followings.follower_id = users.id
                    WHERE
                        followings.following_id = :user_id
                        AND followings.follower_id = :current_user_id
                )
            ;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource()
            .addValue("user_id", 1)
            .addValue("current_user_id", 2)
        val actual = namedParameterJdbcTemplate.update(sql, sqlParams)
        assertThat(actual).isEqualTo(1)
    }

    @Test
    fun `レコードが存在するとき、何もしない`() {
        fun localPrepare() {
            // User を 1 レコード分追加
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val sql1 =
                "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
            val sqlParams1 = MapSqlParameterSource()
                .addValue("id", 1)
                .addValue("email", "dummy@example.com")
                .addValue("username", "dummy")
                .addValue("password", "Passw0rd")
                .addValue("created_at", date)
                .addValue("updated_at", date)
            namedParameterJdbcTemplate.update(sql1, sqlParams1)
            // followings テーブルに 1 行追加
            val sql2 =
                "INSERT INTO followings(id, following_id, follower_id, created_at) VALUES (:id, :following_id, :follower_id, :created_at);"
            val sqlParams2 = MapSqlParameterSource()
                .addValue("id", 1)
                .addValue("following_id", 1)
                .addValue("follower_id", 2)
                .addValue("created_at", date)
            namedParameterJdbcTemplate.update(sql2, sqlParams2)
        }

        val sql = """
            INSERT INTO followings
                (
                    following_id
                    , follower_id 
                    , created_at
                )
            SELECT
                :user_id
                , :current_user_id
                , NOW()
            WHERE
                NOT EXISTS (
                    SELECT
                        1
                    FROM
                        followings
                    JOIN
                        users
                    ON
                        followings.following_id = users.id
                    WHERE
                        followings.following_id = :user_id
                        AND followings.follower_id = :current_user_id
                )
            ;
        """.trimIndent()
        localPrepare()

        val sqlParams = MapSqlParameterSource()
            .addValue("user_id", 1)
            .addValue("current_user_id", 2)
        val actual = namedParameterJdbcTemplate.update(sql, sqlParams)
        assertThat(actual).isEqualTo(0)
    }

    @Test
    fun `username と current_user_id でfollowingにfollowing、followerに存在しないとき followings に insert する`() {
        fun localPrepare() {
            // User を 1 レコード分追加
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val sql1 =
                "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
            val sqlParams1 = MapSqlParameterSource()
                .addValue("id", 1)
                .addValue("email", "dummy@example.com")
                .addValue("username", "dummy")
                .addValue("password", "Passw0rd")
                .addValue("created_at", date)
                .addValue("updated_at", date)
            namedParameterJdbcTemplate.update(sql1, sqlParams1)
            val sql2 =
                "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
            val sqlParams2 = MapSqlParameterSource()
                .addValue("id", 2)
                .addValue("email", "dummy2@example.com")
                .addValue("username", "dummy2")
                .addValue("password", "Passw0rd")
                .addValue("created_at", date)
                .addValue("updated_at", date)
            namedParameterJdbcTemplate.update(sql2, sqlParams2)
        }

        val sql = """
            INSERT INTO followings
                (
                    following_id
                    , follower_id 
                    , created_at
                )
            SELECT
                users.id
                , :current_user_id
                , NOW()
            FROM
                users
            WHERE
                users.username = :username
                AND NOT EXISTS (
                    SELECT
                        1
                    FROM
                        followings
                    JOIN
                        users
                    ON
                        followings.following_id = users.id
                    WHERE
                        users.username = :username
                        AND followings.following_id = users.id
                        AND followings.follower_id = :current_user_id
                )
            ;
        """.trimIndent()
        localPrepare()

        val sqlParams = MapSqlParameterSource()
            .addValue("username", "dummy")
            .addValue("current_user_id", 2)
        val actual = namedParameterJdbcTemplate.update(sql, sqlParams)
        assertThat(actual).isEqualTo(1)
    }

    @Test
    fun `username から userid を検索して、存在するとき followings に insert する`() {
        fun localPrepare() {
            // User を 1 レコード分追加
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val sql1 =
                "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
            val sqlParams1 = MapSqlParameterSource()
                .addValue("id", 1)
                .addValue("email", "dummy@example.com")
                .addValue("username", "dummy")
                .addValue("password", "Passw0rd")
                .addValue("created_at", date)
                .addValue("updated_at", date)
            namedParameterJdbcTemplate.update(sql1, sqlParams1)
            // followings テーブルに 1 行追加
            val sql2 =
                "INSERT INTO followings(id, following_id, follower_id, created_at) VALUES (:id, :following_id, :follower_id, :created_at);"
            val sqlParams2 = MapSqlParameterSource()
                .addValue("id", 1)
                .addValue("following_id", 1)
                .addValue("follower_id", 2)
                .addValue("created_at", date)
            namedParameterJdbcTemplate.update(sql2, sqlParams2)
        }

        val sql = """
            INSERT INTO followings
                (
                    following_id
                    , follower_id 
                    , created_at
                )
            SELECT
                users.id
                , :current_user_id
                , NOW()
            FROM
                users
            WHERE
                NOT EXISTS (
                    SELECT
                        1
                    FROM
                        followings
                    JOIN
                        users
                    ON
                        followings.following_id = users.id
                    WHERE
                        users.username = :username
                        AND followings.following_id = users.id
                        AND followings.follower_id = :current_user_id
                )
            ;
        """.trimIndent()
        localPrepare()

        val sqlParams = MapSqlParameterSource()
            .addValue("username", "dummy")
            .addValue("current_user_id", 2)
        val actual = namedParameterJdbcTemplate.update(sql, sqlParams)
        assertThat(actual).isEqualTo(0)
    }

    @Test
    fun `username が users テーブルに存在しないとき`() {
        val sql = """
            INSERT INTO followings
                (
                    following_id
                    , follower_id 
                    , created_at
                )
            SELECT
                users.id
                , :current_user_id
                , NOW()
            FROM
                users
            WHERE
                users.username = :username
                AND NOT EXISTS (
                    SELECT
                        1
                    FROM
                        followings
                    JOIN
                        users
                    ON
                        followings.following_id = users.id
                    WHERE
                        users.username = :username
                        AND followings.following_id = users.id
                        AND followings.follower_id = :current_user_id
                )
            ;
        """.trimIndent()

        val sqlParams = MapSqlParameterSource()
            .addValue("username", "dummy")
            .addValue("current_user_id", 2)
        val actual = namedParameterJdbcTemplate.update(sql, sqlParams)
        assertThat(actual).isEqualTo(0)
    }
}
