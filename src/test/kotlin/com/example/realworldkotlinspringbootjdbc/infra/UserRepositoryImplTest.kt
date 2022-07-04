package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.UnregisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Password
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import java.text.SimpleDateFormat

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("WithLocalDb")
class UserRepositoryImplTest {
    companion object {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
        fun resetDb() {
            val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
            val sql1 = """
                DELETE FROM users;
                DELETE FROM profiles;
            """.trimIndent()
            namedParameterJdbcTemplate.update(sql1, MapSqlParameterSource())
        }
    }

    @Nested
    @Tag("WithLocalDb")
    class `Register(ユーザー登録)` {
        @BeforeEach
        fun reset() { resetDb() }

        @Test
        fun `EmailやUsernameが利用されておらず(登録できる場合)、登録されたユーザーが戻り値となる`() {
            val user = object : UnregisteredUser {
                override val email: Email get() = Email.newWithoutValidation("dummy@example.com")
                override val password: Password get() = Password.newWithoutValidation("Passw0rd")
                override val username: Username get() = Username.newWithoutValidation("dummy-username")
            }
            val repository = UserRepositoryImpl(namedParameterJdbcTemplate)
            val confirmSql = """SELECT count(email) FROM users WHERE users.email = :email;"""
            val sqlParams = MapSqlParameterSource().addValue("email", user.email.value)

            // 確認: Before/Afterでレコードの数は1増える
            val beforeEmailCount = namedParameterJdbcTemplate.queryForMap(confirmSql, sqlParams)["count"] as Long
            assertThat(beforeEmailCount).isEqualTo(0)

            // 確認: 成功である(UserIdは不定なので、isRightで比較をする。パターンマッチで確認できるならそれでやりたいが、不明なためやっていない)
            val actual = repository.register(user)
            assertThat(actual.isRight()).isTrue

            val afterEmailCount = namedParameterJdbcTemplate.queryForMap(confirmSql, sqlParams)["count"] as Long
            assertThat(afterEmailCount).isEqualTo(1)
        }

        /**
         * 上手く行かない
         * 理由: @Transactional を効かせるには自分で注入するのではなく、SpringBootに注入させる必要がある
         */
        @Disabled
        @Test
        fun `Profileを登録する場合に DataAccessException が投げられた場合、Rollbackされる`() {
            val user = object : UnregisteredUser {
                override val email: Email get() = Email.newWithoutValidation("dummy@example.com")
                override val password: Password get() = Password.newWithoutValidation("Passw0rd")
                override val username: Username get() = Username.newWithoutValidation("dummy-username")
            }
            val updateThrowDataAccessExceptionNamedParameterJdbcTemplate = object : NamedParameterJdbcTemplate(DbConnection.dataSource()) {
                override fun update(sql: String, paramSource: SqlParameterSource): Int = throw object : DataAccessException("Hello") {}
            }
            val repository = UserRepositoryImpl(updateThrowDataAccessExceptionNamedParameterJdbcTemplate)

            val confirmSql1 = """SELECT count(id) FROM users;"""
            val confirmSql2 = """SELECT count(id) FROM profiles;"""
            val sqlParams = MapSqlParameterSource()
            val beforeEmailCount1 = namedParameterJdbcTemplate.queryForMap(confirmSql1, sqlParams)["count"] as Long
            val beforeEmailCount2 = namedParameterJdbcTemplate.queryForMap(confirmSql2, sqlParams)["count"] as Long
            assertThat(beforeEmailCount1).isEqualTo(0)
            assertThat(beforeEmailCount2).isEqualTo(0)

            val actual = repository.register(user)
            assertThat(actual.isRight()).isFalse

            val afterEmailCount1 = namedParameterJdbcTemplate.queryForMap(confirmSql1, sqlParams)["count"] as Long
            val afterEmailCount2 = namedParameterJdbcTemplate.queryForMap(confirmSql2, sqlParams)["count"] as Long
            assertThat(afterEmailCount1).isEqualTo(0)
            assertThat(afterEmailCount2).isEqualTo(0)
        }

        @Test
        fun `Emailが既に登録されていた場合、その旨のエラーが戻り値となる`() {
            fun localPrepare(email: Email) { // 事前に User を 1 レコード分追加
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
                val sql1 =
                    "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
                val sqlParams1 = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("email", email.value)
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
            val duplicatedEmail = Email.newWithoutValidation("dummy@example.com")
            localPrepare(duplicatedEmail)
            val repository = UserRepositoryImpl(namedParameterJdbcTemplate)
            val user = object : UnregisteredUser {
                override val email: Email get() = Email.newWithoutValidation("dummy@example.com")
                override val password: Password get() = Password.newWithoutValidation("Passw0rd")
                override val username: Username get() = Username.newWithoutValidation("dummy-username")
            }
            val confirmRecordCountSql = """SELECT count(*) FROM users WHERE email = '${duplicatedEmail.value}';"""

            // 確認: Before/After で レコードの数が変わってないこと
            val beforeRecordCount = namedParameterJdbcTemplate.queryForMap(confirmRecordCountSql, MapSqlParameterSource())["count"] as Long
            assertThat(beforeRecordCount).isEqualTo(1L)

            // 確認: エラーであること
            val actual = repository.register(user)
            val expected = UserRepository.RegisterError.AlreadyRegisteredEmail(duplicatedEmail).left()
            assertThat(actual).isEqualTo(expected)

            val afterRecordCount = namedParameterJdbcTemplate.queryForMap(confirmRecordCountSql, MapSqlParameterSource())["count"] as Long
            assertThat(afterRecordCount).isEqualTo(1L)
        }

        @Disabled
        @Test
        fun `Usernameが既に登録されていた場合、その旨のエラーが戻り値となる`() {
            TODO()
        }
    }

    @Nested
    @Tag("WithLocalDb")
    class `findByEmailWithPassword(Emailでユーザー検索 with Password)` {
        @BeforeEach
        fun reset() { resetDb() }

        @Test
        fun `該当するユーザーが存在する場合、パスワード付きでユーザーが戻り値となる`() {
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
            val repository = UserRepositoryImpl(namedParameterJdbcTemplate)

            val searchingEmail = Email.newWithoutValidation("dummy@example.com")
            when (val actual = repository.findByEmailWithPassword(searchingEmail)) {
                is Left -> assert(false)
                is Right -> assertThat(actual.value.first.email).isEqualTo(searchingEmail)
            }
        }

        @Test
        fun `該当するユーザーが存在しない場合、その旨のエラーが戻り値となる`() {
            val repository = UserRepositoryImpl(namedParameterJdbcTemplate)

            val searchingEmail = Email.newWithoutValidation("notfound@example.com")
            when (val actual = repository.findByEmailWithPassword(searchingEmail)) {
                is Left -> assertThat(actual.value).isEqualTo(UserRepository.FindByEmailWithPasswordError.NotFound(searchingEmail))
                is Right -> assert(false)
            }
        }
    }
}
