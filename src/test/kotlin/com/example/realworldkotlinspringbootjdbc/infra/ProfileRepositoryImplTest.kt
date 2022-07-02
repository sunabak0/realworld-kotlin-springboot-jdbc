package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.domain.Profile
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
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
    fun `ProfileRepository show() で 1 件取得時の正常系`() {
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

    @Test
    fun `ProfileRepository show() で 0 件だったときの異常系`() {
        val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)

        val expect =
            ProfileRepository.ShowError.NotFoundProfileByUsername(Username.newWithoutValidation("dummy-username"))
        when (val actual = profileRepository.show(Username.newWithoutValidation("dummy-username"))) {
            is Left -> assertThat(actual.value).isEqualTo(expect)
            is Right -> assert(false)
        }
    }

    @Test
    fun `ProfileRepository show() で namedParameterJdbcTemplate が Exception を throw したときの異常系`() {
        TODO()
        // fun dataSource(): DataSource {
        //     val hikariConfig = HikariConfig()
        //     hikariConfig.jdbcUrl = "jdbc:postgresql://127.0.0.1:5432/realworld-db"
        //     hikariConfig.username = "realworld-user"
        //     hikariConfig.password = "realworld-pass"
        //     hikariConfig.connectionTimeout = java.lang.Long.valueOf(500)
        //     hikariConfig.isAutoCommit = true
        //     hikariConfig.transactionIsolation = "TRANSACTION_READ_COMMITTED"
        //     hikariConfig.poolName = "realworldPool01"
        //     hikariConfig.maximumPoolSize = 10
        //     return HikariDataSource(hikariConfig)
        // }
        //
        // val piyo = MutableList<MutableMap<String, Any>>(1, (index: Int) -> <MutableMap<String, Any )
        // val Hoge = object : NamedParameterJdbcTemplate(dataSource()) {
        //     override fun queryForList(
        //         sql: String,
        //         paramMap: MutableMap<String, *>
        //     ): MutableList<MutableMap<String, Any>> {
        //         throw object : DataAccessException("message")
        //         return piyo
        //     }
        // }
        //
        // val profileRepository = ProfileRepositoryImpl(Hoge)
    }

    @Test
    fun `ProfileRepository follow() で戻り値が Unit の正常系`() {
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
        val confirmFollowingsSql =
            "SELECT COUNT(*) AS CNT FROM followings WHERE follower_id = :current_user_id AND following_id = :user_id"
        val confirmFollowingsParam = MapSqlParameterSource()
            .addValue("user_id", 1)
            .addValue("current_user_id", 2)

        /**
         * 実行前に挿入されていないことを確認
         */
        val beforeResult = namedParameterJdbcTemplate.queryForList(confirmFollowingsSql, confirmFollowingsParam)
        assertThat(beforeResult[0]["CNT"]).isEqualTo(0L)

        /**
         * 実行時エラーが発生しないことを確認
         */
        val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)
        when (val actual = profileRepository.follow(Username.newWithoutValidation("dummy-username"), UserId(2))) {
            is Left -> assert(false)
            is Right -> assertThat(actual.value).isEqualTo(Unit)
        }

        /**
         * 実行後に1件だけ挿入されていることを確認
         */
        val afterResult = namedParameterJdbcTemplate.queryForList(confirmFollowingsSql, confirmFollowingsParam)
        assertThat(afterResult[0]["CNT"]).isEqualTo(1L)
    }
}
