package com.example.realworldkotlinspringbootjdbc.sandbox.db

import com.example.realworldkotlinspringbootjdbc.repository.DbConnection
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource

@Tag("WithLocalDb")
class DbConnectionTest {
    @Test
    fun `DBへ接続できる`() {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
        val sql = "SELECT year FROM (VALUES (2020),(2021),(2022)) v(year);"
        val sqlParams = MapSqlParameterSource()
        val actual = namedParameterJdbcTemplate.queryForList(sql, sqlParams)
        val expected = listOf(mapOf("year" to 2020), mapOf("year" to 2021), mapOf("year" to 2022))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `emailが存在していないかを確認するテスト`() {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
        val sql = """
            SELECT count(id)
            FROM (VALUES (1, 'foo@example.com'),(2, 'bar@example.com')) v(id, email)
            WHERE email = :email;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource()
            .addValue("email", "nothing@example.com")
        val actual = namedParameterJdbcTemplate.queryForMap(sql, sqlParams)
        val expected = mapOf("count" to 0)
        assertThat(actual).isEqualTo(expected)
    }
}
