package com.example.realworldkotlinspringbootjdbc.sandbox.db

import com.example.realworldkotlinspringbootjdbc.infra.UserRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.transaction.annotation.Transactional
import java.util.Date

@Tag("WithLocalDb")
@SpringBootTest
class ConfirmRollbackWithSpringBoot(val userRepository: UserRepositoryImpl) {
    val namedParameterJdbcTemplate = userRepository.namedParameterJdbcTemplate
    @BeforeEach
    fun resetDb() {
        namedParameterJdbcTemplate.update("DELETE FROM users;", MapSqlParameterSource())
    }

    @Disabled
    @Test
    fun `rollbackを確認する`() {
        val confirmSql = """SELECT count(id) FROM users;"""
        val sqlParams = MapSqlParameterSource()

        // Before
        val beforeEmailCount = namedParameterJdbcTemplate.queryForMap(confirmSql, sqlParams)["count"] as Long
        assertThat(beforeEmailCount).isEqualTo(0L)

        try {
            tryInsert2timesUsingSameEmail()
        } catch (_: Throwable) {
        }

        // After
        val afterEmailCount = namedParameterJdbcTemplate.queryForMap(confirmSql, sqlParams)["count"] as Long
        assertThat(afterEmailCount).isEqualTo(0L)
    }

    @Transactional
    fun tryInsert2timesUsingSameEmail() {
        val date = Date()
        val sql = """
            INSERT
            INTO users(id, email, username, password, created_at, updated_at) 
            VALUES (:id, :email, :username, :password, :created_at, :updated_at);
        """.trimIndent()
        val sqlParams1 = MapSqlParameterSource()
            .addValue("id", 1)
            .addValue("email", "dummy@example.com")
            .addValue("username", "dummy1")
            .addValue("password", "Passw0rd")
            .addValue("created_at", date)
            .addValue("updated_at", date)

        namedParameterJdbcTemplate.update(sql, sqlParams1)
        namedParameterJdbcTemplate.update(sql, sqlParams1)
    }
}
