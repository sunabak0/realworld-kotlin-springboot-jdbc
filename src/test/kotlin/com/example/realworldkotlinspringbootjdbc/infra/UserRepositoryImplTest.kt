package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
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
class UserRepositoryImplTest {
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
    fun `正常系hogehogehoge`() {
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

        val userRepository = UserRepositoryImpl()
        when (val actual = userRepository.findByUserId(UserId(1))) {
            is Left -> assert(false)
            is Right -> assertThat(actual.value.userId).isEqualTo(UserId(1))
        }
    }
}
