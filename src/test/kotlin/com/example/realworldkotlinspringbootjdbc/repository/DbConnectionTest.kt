package com.example.realworldkotlinspringbootjdbc.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource

@Tag("WithLocalDb")
class DbConnectionTest {
    @Test
    fun `DB接続テスト`() {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
        val sql = "SELECT year FROM (VALUES (2020),(2021),(2022)) v(year);"
        val sqlParams = MapSqlParameterSource()
        val actual = namedParameterJdbcTemplate.queryForList(sql, sqlParams)
        val expected = listOf(mapOf("year" to 2020), mapOf("year" to 2021), mapOf("year" to 2022))
        assertThat(actual).isEqualTo(expected)
    }
}
