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
class SelectJoin {
    @BeforeEach
    @AfterEach
    fun resetDb() {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
        val sql1 = """
            DELETE FROM users;
            DELETE FROM profiles;
        """.trimIndent()
        namedParameterJdbcTemplate.update(sql1, MapSqlParameterSource())
    }

    private val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate

    @Test
    fun `レコードが見つからない場合、空のListが戻り値となる`() {
        val sql = "SELECT * FROM users JOIN profiles ON users.id = profiles.user_id WHERE users.email = :email;"
        val sqlParams = MapSqlParameterSource()
            .addValue("email", "dummy@example.com")
        val actual = namedParameterJdbcTemplate.queryForList(sql, sqlParams)
        assertThat(actual).isEmpty()
    }

    @Test
    fun `レコードが 1つ見つかる場合、sizeが1のListが戻り値となる`() {
        fun localPrepare() { // 事前に User を 1 レコード分追加
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
                "INSERT INTO profiles(id, user_id, bio, image, created_at, updated_at) VALUES (:id, :user_id, :bio, :image, :created_at, :updated_at);"
            val sqlParams2 = MapSqlParameterSource()
                .addValue("id", 1)
                .addValue("user_id", 1)
                .addValue("bio", "dummy")
                .addValue("image", "dummy")
                .addValue("created_at", date)
                .addValue("updated_at", date)
            namedParameterJdbcTemplate.update(sql2, sqlParams2)
        }
        localPrepare()

        val sql = "SELECT * FROM users JOIN profiles ON users.id = profiles.user_id WHERE users.email = :email;"
        val sqlParams = MapSqlParameterSource()
            .addValue("email", "dummy@example.com")
        val actual = namedParameterJdbcTemplate.queryForList(sql, sqlParams)
        assertThat(actual).hasSize(1)
    }
}
