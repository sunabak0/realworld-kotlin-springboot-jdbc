package com.example.realworldkotlinspringbootjdbc.sandbox.db

import com.example.realworldkotlinspringbootjdbc.infra.DbConnection
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.text.SimpleDateFormat

@Repository
class DIRepositoryBySpringBoot(
    @Autowired val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    /**
     * この @Transactional を効かせるために
     * 上の@Repositoryアノテーションを付けて、SpringBootにDIさせる必要がある
     */
    @Transactional
    fun failInsert() {
        /**
         * 6回目の INSERT でemailを重複させているため、失敗
         */
        val sql = """
            INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (1, '1@example.com', '1username', 'Passw0rd', :created_at, :updated_at);
            INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (2, '2@example.com', '2username', 'Passw0rd', :created_at, :updated_at);
            INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (3, '3@example.com', '3username', 'Passw0rd', :created_at, :updated_at);           
            INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (4, '4@example.com', '4username', 'Passw0rd', :created_at, :updated_at);           
            INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (5, '5@example.com', '5username', 'Passw0rd', :created_at, :updated_at);           
            INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (6, '1@example.com', '6username', 'Passw0rd', :created_at, :updated_at);           
        """.trimIndent()
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-03T00:00:00+09:00")
        val params = MapSqlParameterSource()
            .addValue("created_at", date)
            .addValue("updated_at", date)
        namedParameterJdbcTemplate.update(sql, params)
    }

    /**
     * success:
     */
    @Transactional
    fun insertOnceUserAndProfile(success: Boolean = true): MutableMap<String, Any> {
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
        val sql = """
            WITH inserted_user AS (
                INSERT INTO users(
                    id
                    , email
                    , username
                    , password
                    , created_at
                    , updated_at
                ) VALUES (
                    :user_id
                    , :email
                    , :username
                    , :password
                    , :created_at
                    , :updated_at
                ) RETURNING
                    id
            )
            INSERT INTO profiles(
                user_id
                , bio
                , image
                , created_at
                , updated_at
            )
            SELECT
                id
                , :bio
                , :image
                , :created_at
                , :updated_at
                FROM inserted_user
            RETURNING
                user_id
            ;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource()
            .addValue("user_id", 1)
            .addValue("email", "dummy@example.com")
            .addValue("username", "dummy-username")
            .addValue("password", "Passw0rd")
            .addValue("bio", "dummy-bio")
            .addValue("created_at", date)
            .addValue("updated_at", date)

        /**
         * success が false の場合、 image が NULL になって失敗する
         * image カラムが NOT NULL 制約 なため
         */
        if (success) { sqlParams.addValue("image", "dummy-image") } else { sqlParams.addValue("image", null) }

        return namedParameterJdbcTemplate.queryForMap(sql, sqlParams)
    }
}

/**
 * @SpringBootTest をつけている理由
 * - @Transactional を効かせるために、自分でではなく、SpringBootにDIさせる必要があるため
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("WithLocalDb")
class OnceSqlMultiTable(
    @Autowired val repo: DIRepositoryBySpringBoot
) {
    val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate

    @BeforeEach
    @AfterEach
    fun resetDb() {
        val sql1 = """
            DELETE FROM users;
            DELETE FROM profiles;
        """.trimIndent()
        namedParameterJdbcTemplate.update(sql1, MapSqlParameterSource())
    }

    @Test
    fun `1発でusersテーブルとprofilesテーブルにレコードをINSERTする`() {
        val actual = repo.insertOnceUserAndProfile()
        val expected = mapOf("user_id" to 1L)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `1発でusersテーブルとprofilesテーブルにレコードをINSERTしようとして、失敗させて、Rollbackされていることを確認する`() {
        try {
            repo.insertOnceUserAndProfile(success = false)
        } catch (_: Throwable) {}
        val confirmSql = """
            SELECT
                count(id)
            FROM
                users
            ;
        """.trimIndent()
        val actual = namedParameterJdbcTemplate.queryForMap(confirmSql, MapSqlParameterSource())
        val expected = mapOf("count" to 0L)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `1発ではないだろうけど、1回のクエリでusersテーブルとprofilesテーブルにレコードをUPDATEする`() {
        val map = repo.insertOnceUserAndProfile()
        val userId = map["user_id"].toString().toInt()

        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-02T00:00:00+09:00")
        val sql = """
            UPDATE users
            SET
                email = :email
                , username = :username
                , updated_at = :updated_at
            WHERE
                id = :user_id
            ;
            UPDATE
                profiles
            SET 
                bio = :bio
                , image = :image
                , updated_at = :updated_at
            WHERE
                user_id = :user_id
            ;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource()
            .addValue("user_id", userId)
            .addValue("email", "dummy2@example.com")
            .addValue("username", "dummy-username2")
            .addValue("bio", "dummy-bio2")
            .addValue("image", "dummy-image2")
            .addValue("updated_at", date)
        namedParameterJdbcTemplate.update(sql, sqlParams)

        val confirmSql = """
            SELECT
                email
            FROM
                users
            WHERE
                id = :user_id
            ;
        """.trimIndent()
        val actual = namedParameterJdbcTemplate.queryForMap(confirmSql, sqlParams)
        val expected = mapOf("email" to "dummy2@example.com")
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Rollbackの確認`() {
        try {
            repo.failInsert()
        } catch (_: Throwable) {
            /**
             * この時点でもう、Rollbackされている
             */
        }
        val confirmSql = """
            SELECT
                count(id)
            FROM
                users
            ;
        """.trimIndent()
        val actual = namedParameterJdbcTemplate.queryForMap(confirmSql, MapSqlParameterSource())
        val expected = mapOf("count" to 0L)
        assertThat(actual).isEqualTo(expected)
    }
}
