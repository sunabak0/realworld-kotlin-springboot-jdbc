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
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
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
    fun `ログイン時の ProfileRepository show() で 1 件取得時、フォロー済のときの正常系`() {
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

            val insertFollowingsSql =
                "INSERT INTO followings(id, following_id, follower_id, created_at) VALUES (:id, :following_id, :follower_id, :created_at);"
            val insertFollowingsSqlParams = MapSqlParameterSource()
                .addValue("id", 1)
                .addValue("following_id", 1)
                .addValue("follower_id", 2)
                .addValue("created_at", date)
            namedParameterJdbcTemplate.update(insertFollowingsSql, insertFollowingsSqlParams)
        }
        localPrepare()

        val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)

        val expect = Profile.newWithoutValidation(
            Username.newWithoutValidation("dummy-username"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
            true
        )
        when (val actual = profileRepository.show(Username.newWithoutValidation("dummy-username"), UserId(2))) {
            is Left -> assert(false)
            is Right -> assertThat(actual.value).isEqualTo(expect)
        }
    }

    @Test
    fun `ログイン時の ProfileRepository show() で 1 件取得時、未フォローのときの正常系`() {
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
        when (val actual = profileRepository.show(Username.newWithoutValidation("dummy-username"), UserId(2))) {
            is Left -> assert(false)
            is Right -> assertThat(actual.value).isEqualTo(expect)
        }
    }

    @Test
    fun `ログイン時の ProfileRepository show() で 0 件だったときの異常系`() {
        val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)

        val expect =
            ProfileRepository.ShowError.NotFoundProfileByUsername(
                Username.newWithoutValidation("dummy-username"),
                UserId(2)
            )
        when (val actual = profileRepository.show(Username.newWithoutValidation("dummy-username"), UserId(2))) {
            is Left -> assertThat(actual.value).isEqualTo(expect)
            is Right -> assert(false)
        }
    }

    @Test
    fun `ログイン時の ProfileRepository show() で namedParameterJdbcTemplate が Exception を throw したときの異常系`() {
        val throwDatabaseAccessException = object : NamedParameterJdbcTemplate(DbConnection.dataSource()) {
            override fun queryForList(
                sql: String,
                paramMap: MutableMap<String, *>
            ): MutableList<MutableMap<String, Any>> {
                throw object : DataAccessException("message") {}
            }
        }
        val profileRepository = ProfileRepositoryImpl(throwDatabaseAccessException)
    }

    @Test
    fun `未ログイン時の ProfileRepository show() で 1 件取得時の正常系`() {
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
    fun `未ログイン時に ProfileRepository show() で 0 件だったときの異常系`() {
        val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)

        val expect =
            ProfileRepository.ShowWithoutAuthorizedError.NotFoundProfileByUsername(
                Username.newWithoutValidation("dummy-username"),
            )
        when (val actual = profileRepository.show(Username.newWithoutValidation("dummy-username"))) {
            is Left -> assertThat(actual.value).isEqualTo(expect)
            is Right -> assert(false)
        }
    }

    @Test
    fun `未ログイン時の  ProfileRepository show() で namedParameterJdbcTemplate が Exception を throw したときの異常系`() {
        TODO()
    }

    @Test
    fun `ProfileRepository follow() で戻り値が Profile、followings テーブルに登録されていないため、挿入されるときの正常系`() {
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
         * 戻り値がフォロー済の Profile であることを確認
         */
        val expectProfile = Profile.newWithoutValidation(
            Username.newWithoutValidation("dummy-username"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
            true
        )
        val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)
        when (val actual = profileRepository.follow(Username.newWithoutValidation("dummy-username"), UserId(2))) {
            is Left -> assert(false)
            is Right -> assertThat(actual.value).isEqualTo(expectProfile)
        }

        /**
         * 実行後に1件だけ挿入されていることを確認
         */
        val afterResult = namedParameterJdbcTemplate.queryForList(confirmFollowingsSql, confirmFollowingsParam)
        assertThat(afterResult[0]["CNT"]).isEqualTo(1L)
    }

    @Test
    fun `ProfileRepository follow() で戻り値が Profile、followings テーブルに登録されているため、挿入されないときの正常系`() {
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

            // followings テーブルにすでにフォロー済のレコードを追加
            val sql2 =
                "INSERT INTO followings(id, following_id, follower_id, created_at) VALUES (:id, :following_id, :follower_id, :created_at);"
            val sqlParams2 = MapSqlParameterSource()
                .addValue("id", 1)
                .addValue("following_id", 1)
                .addValue("follower_id", 2)
                .addValue("created_at", date)
            namedParameterJdbcTemplate.update(sql2, sqlParams2)
        }
        localPrepare()
        val confirmFollowingsSql =
            "SELECT COUNT(*) AS CNT FROM followings WHERE follower_id = :current_user_id AND following_id = :user_id"
        val confirmFollowingsParam = MapSqlParameterSource()
            .addValue("user_id", 1)
            .addValue("current_user_id", 2)

        /**
         * 実行前に既に挿入されていることを確認
         */
        val beforeResult = namedParameterJdbcTemplate.queryForList(confirmFollowingsSql, confirmFollowingsParam)
        assertThat(beforeResult[0]["CNT"]).isEqualTo(1L)

        /**
         * 戻り値がフォロー済の Profile であることを確認
         */
        val expectProfile = Profile.newWithoutValidation(
            Username.newWithoutValidation("dummy-username"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
            true
        )
        val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)
        when (val actual = profileRepository.follow(Username.newWithoutValidation("dummy-username"), UserId(2))) {
            is Left -> assert(false)
            is Right -> assertThat(actual.value).isEqualTo(expectProfile)
        }

        /**
         * 実行後に挿入されていないことを確認
         */
        val afterResult = namedParameterJdbcTemplate.queryForList(confirmFollowingsSql, confirmFollowingsParam)
        assertThat(afterResult[0]["CNT"]).isEqualTo(1L)
    }

    @Test
    fun `ProfileRepository follow() でDBエラーが発生したときの異常系`() {
        TODO()
    }

    @Test
    fun `ProfileRepository unfollow() で戻り値が Profile の正常系`() {
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

            /**
             * followings テーブルにすでにフォロー済のレコードを追加
             */
            val insertFollowingsFollowerSql =
                "INSERT INTO followings(id, following_id, follower_id, created_at) VALUES (:id, :following_id, :follower_id, :created_at);"
            val insertFollowingsFollowerSqlParams = MapSqlParameterSource()
                .addValue("id", 1)
                .addValue("following_id", 1)
                .addValue("follower_id", 2)
                .addValue("created_at", date)
            namedParameterJdbcTemplate.update(insertFollowingsFollowerSql, insertFollowingsFollowerSqlParams)
            /**
             * 他の follower_id が削除されないか確認のため他のフォロワーを追加
             */
            val insertFollowingsOtherFollowerSqlParams = MapSqlParameterSource()
                .addValue("id", 2)
                .addValue("following_id", 1)
                .addValue("follower_id", 3)
                .addValue("created_at", date)
            namedParameterJdbcTemplate.update(insertFollowingsFollowerSql, insertFollowingsOtherFollowerSqlParams)
        }
        localPrepare()
        val confirmFollowingsSql =
            "SELECT COUNT(*) AS CNT FROM followings WHERE follower_id = :current_user_id AND following_id = :user_id"
        val confirmFollowingsParam = MapSqlParameterSource()
            .addValue("user_id", 1)
            .addValue("current_user_id", 2)

        /**
         * 実行前に1件存在していることを確認
         */
        val beforeResult = namedParameterJdbcTemplate.queryForList(confirmFollowingsSql, confirmFollowingsParam)
        assertThat(beforeResult[0]["CNT"]).isEqualTo(1L)

        /**
         * 戻り値が未フォローの Profile であることを確認
         */
        val expectProfile = Profile.newWithoutValidation(
            Username.newWithoutValidation("dummy-username"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
            false
        )
        val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)
        when (val actual = profileRepository.unfollow(Username.newWithoutValidation("dummy-username"), UserId(2))) {
            is Left -> assert(false)
            is Right -> assertThat(actual.value).isEqualTo(expectProfile)
        }

        /**
         * 実行後に削除されていることを確認
         */
        val afterResult = namedParameterJdbcTemplate.queryForList(confirmFollowingsSql, confirmFollowingsParam)
        assertThat(afterResult[0]["CNT"]).isEqualTo(0L)

        /**
         * 余分な行が削除されていないか確認。オリジナル 2 行 -> 実行後 1 行
         */
        val confirmAllFollowingsSql =
            "SELECT COUNT(*) AS CNT FROM followings WHERE following_id = :user_id"
        val confirmAllFollowingsParam = MapSqlParameterSource()
            .addValue("user_id", 1)
        val confirmAllFollowingsResult = namedParameterJdbcTemplate.queryForList(confirmAllFollowingsSql, confirmAllFollowingsParam)
        assertThat(confirmAllFollowingsResult[0]["CNT"]).isEqualTo(1L)
    }

    @Test
    fun `ProfileRepository unfollow() でDBエラーの異常系`() {
        TODO()
    }
}
