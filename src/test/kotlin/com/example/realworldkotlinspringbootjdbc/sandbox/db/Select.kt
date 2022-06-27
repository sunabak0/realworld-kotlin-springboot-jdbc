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
class Select {
    @BeforeEach
    @AfterEach
    fun resetDb() {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
        val sql1 = """
            DELETE FROM users;
        """.trimIndent()
        namedParameterJdbcTemplate.update(sql1, MapSqlParameterSource())
    }

    private val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate

    @Test
    fun `レコードが見つからない場合、空のListが戻り値となる`() {
        val sql = "SELECT * FROM users WHERE users.email = :email;"
        val sqlParams = MapSqlParameterSource()
            .addValue("email", "dummy@example.com")
        val actual = namedParameterJdbcTemplate.queryForList(sql, sqlParams)
        assertThat(actual).isEmpty()
    }

    @Test
    fun `レコードが1つ見つかる場合、sizeが1のListが戻り値となる`() {
        fun localPrepare() { // 事前にUserを1レコード分追加
            val sql = "INSERT INTO users(email, username, password, created_at, updated_at) VALUES (:email, :username, :password, :created_at, :updated_at);"
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val sqlParams = MapSqlParameterSource()
                .addValue("email", "dummy@example.com")
                .addValue("username", "dummy")
                .addValue("password", "Passw0rd")
                .addValue("created_at", date)
                .addValue("updated_at", date)
            namedParameterJdbcTemplate.update(sql, sqlParams)
        }
        localPrepare()

        val sql = "SELECT * FROM users WHERE users.email = :email;"
        val sqlParams = MapSqlParameterSource()
            .addValue("email", "dummy@example.com")
        val actual = namedParameterJdbcTemplate.queryForList(sql, sqlParams)
        assertThat(actual).hasSize(1)
    }
}
