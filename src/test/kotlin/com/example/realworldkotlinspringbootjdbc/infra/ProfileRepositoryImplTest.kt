package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.domain.Profile
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
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
class ProfileRepositoryImplTest {
    @BeforeEach
    @AfterEach
    fun resetDb() {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
        val sql = """
            DELETE FROM users;
            DELETE FROM profiles;
            DELETE FROM followings;
        """.trimIndent()
        namedParameterJdbcTemplate.update(sql, MapSqlParameterSource())
    }

    private val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate

    @Test
    fun `ProfileRepository show() で 1 件見つかったときの正常系hoge`() {
        fun localPrepare() {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")

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
            val insertProfileSqlParams = MapSqlParameterSource()
                .addValue("id", 1)
                .addValue("user_id", 1)
                .addValue("bio", "dummy-bio")
                .addValue("image", "dummy-image")
                .addValue("created_at", date)
                .addValue("updated_at", date)
            namedParameterJdbcTemplate.update(insertProfileSql, insertProfileSqlParams)
        }
        localPrepare()

        val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)

        val expect = Profile.newWithoutValidation(
            Username.newWithoutValidation("dummy-username"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
            false
        )
        when (val actual = profileRepository.show(Username.newWithoutValidation("dummy-username"))) {
            is Left -> assert(false)
            is Right -> assertThat(actual.value).isEqualTo(expect)
        }
    }
}
