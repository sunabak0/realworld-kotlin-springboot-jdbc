package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Option
import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.toOption
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.infra.helper.SeedData
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.text.SimpleDateFormat
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("WithLocalDb")
class ProfileRepositoryImplTest {
    companion object {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
        fun resetDb() {
            val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
            val sql = """
                DELETE FROM users;
                DELETE FROM profiles;
                DELETE FROM followings;
            """.trimIndent()
            namedParameterJdbcTemplate.update(sql, MapSqlParameterSource())
        }
    }

    private val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate

    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class `Show(プロフィールを表示)` {
        @BeforeEach
        @AfterAll
        fun reset() {
            resetDb()
        }

        @Test
        fun `正常系-ログイン済み-フォロー済、 OtherUser が戻り値`() {
            fun localPrepare() {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")

                val insertUserSql =
                    "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
                val insertUserSqlParams =
                    MapSqlParameterSource().addValue("id", 1).addValue("email", "dummy@example.com")
                        .addValue("username", "dummy-username").addValue("password", "Passw0rd")
                        .addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertUserSql, insertUserSqlParams)

                val insertProfileSql =
                    "INSERT INTO profiles(id, user_id, bio, image, created_at, updated_at) VALUES (:id, :user_id, :bio, :image, :created_at, :updated_at);"
                val insertProfileSqlParams1 =
                    MapSqlParameterSource().addValue("id", 1).addValue("user_id", 1).addValue("bio", "dummy-bio")
                        .addValue("image", "dummy-image").addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertProfileSql, insertProfileSqlParams1)
                val insertProfileSqlParams2 =
                    MapSqlParameterSource().addValue("id", 2).addValue("user_id", 2).addValue("bio", "dummy-bio")
                        .addValue("image", "dummy-image").addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertProfileSql, insertProfileSqlParams2)

                val insertFollowingsSql =
                    "INSERT INTO followings(id, following_id, follower_id, created_at) VALUES (:id, :following_id, :follower_id, :created_at);"
                val insertFollowingsSqlParams =
                    MapSqlParameterSource().addValue("id", 1).addValue("following_id", 1).addValue("follower_id", 2)
                        .addValue("created_at", date)
                namedParameterJdbcTemplate.update(insertFollowingsSql, insertFollowingsSqlParams)
            }
            localPrepare()

            val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)

            val expected = OtherUser.newWithoutValidation(
                UserId(1),
                Username.newWithoutValidation("dummy-username"),
                Bio.newWithoutValidation("dummy-bio"),
                Image.newWithoutValidation("dummy-image"),
                following = true
            )
            when (val actual = profileRepository.show(Username.newWithoutValidation("dummy-username"), UserId(2))) {
                is Left -> assert(false)
                is Right -> assertThat(actual.value).isEqualTo(expected)
            }
        }

        @Test
        fun `正常系-ログイン済み-未フォローのときの OtherUser が戻り値`() {
            fun localPrepare() {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")

                val insertUserSql =
                    "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
                val insertUserSqlParams =
                    MapSqlParameterSource().addValue("id", 1).addValue("email", "dummy@example.com")
                        .addValue("username", "dummy-username").addValue("password", "Passw0rd")
                        .addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertUserSql, insertUserSqlParams)

                val insertProfileSql =
                    "INSERT INTO profiles(id, user_id, bio, image, created_at, updated_at) VALUES (:id, :user_id, :bio, :image, :created_at, :updated_at);"
                val insertProfileSqlParams1 =
                    MapSqlParameterSource().addValue("id", 1).addValue("user_id", 1).addValue("bio", "dummy-bio")
                        .addValue("image", "dummy-image").addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertProfileSql, insertProfileSqlParams1)
                val insertProfileSqlParams2 =
                    MapSqlParameterSource().addValue("id", 2).addValue("user_id", 2).addValue("bio", "dummy-bio")
                        .addValue("image", "dummy-image").addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertProfileSql, insertProfileSqlParams2)
            }
            localPrepare()

            val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)

            val expected = OtherUser.newWithoutValidation(
                UserId(1),
                Username.newWithoutValidation("dummy-username"),
                Bio.newWithoutValidation("dummy-bio"),
                Image.newWithoutValidation("dummy-image"),
                following = false
            )
            when (val actual = profileRepository.show(Username.newWithoutValidation("dummy-username"), UserId(2))) {
                is Left -> assert(false)
                is Right -> assertThat(actual.value).isEqualTo(expected)
            }
        }

        @Test
        fun `異常系-ログイン済み、NotFoundProfileByUsername が戻り値`() {
            val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)

            val expected = ProfileRepository.ShowError.NotFoundProfileByUsername(
                Username.newWithoutValidation("dummy-username"), UserId(2)
            )
            when (val actual = profileRepository.show(Username.newWithoutValidation("dummy-username"), UserId(2))) {
                is Left -> assertThat(actual.value).isEqualTo(expected)
                is Right -> assert(false)
            }
        }

        @Test
        @Disabled
        fun `異常系-ログイン済み、UnexpectedError が戻り値`() {
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
        fun `正常系-未ログイン、OtherUser が戻り値`() {
            fun localPrepare() {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")

                val insertUserSql =
                    "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
                val insertUserSqlParams =
                    MapSqlParameterSource().addValue("id", 1).addValue("email", "dummy@example.com")
                        .addValue("username", "dummy-username").addValue("password", "Passw0rd")
                        .addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertUserSql, insertUserSqlParams)

                val insertProfileSql =
                    "INSERT INTO profiles(id, user_id, bio, image, created_at, updated_at) VALUES (:id, :user_id, :bio, :image, :created_at, :updated_at);"
                val insertProfileSqlParams =
                    MapSqlParameterSource().addValue("id", 1).addValue("user_id", 1).addValue("bio", "dummy-bio")
                        .addValue("image", "dummy-image").addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertProfileSql, insertProfileSqlParams)
            }
            localPrepare()

            val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)

            val expected = OtherUser.newWithoutValidation(
                UserId(1),
                Username.newWithoutValidation("dummy-username"),
                Bio.newWithoutValidation("dummy-bio"),
                Image.newWithoutValidation("dummy-image"),
                following = false
            )
            when (val actual = profileRepository.show(Username.newWithoutValidation("dummy-username"))) {
                is Left -> assert(false)
                is Right -> assertThat(actual.value).isEqualTo(expected)
            }
        }

        @Test
        fun `異常系-未ログイン、NotFoundProfileByUsername が戻り値`() {
            val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)

            val expected = ProfileRepository.ShowWithoutAuthorizedError.NotFoundProfileByUsername(
                Username.newWithoutValidation("dummy-username"),
            )
            when (val actual = profileRepository.show(Username.newWithoutValidation("dummy-username"))) {
                is Left -> assertThat(actual.value).isEqualTo(expected)
                is Right -> assert(false)
            }
        }

        @Test
        @Disabled
        fun `異常系-未ログイン、UnexpectedError が戻り値`() {
            TODO()
        }
    }

    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class `Follow(他ユーザーをフォロー)` {
        @BeforeEach
        @AfterAll
        fun reset() {
            resetDb()
        }

        @Test
        fun `正常系-未フォロー、OtherUser が戻り値-followings テーブルに登録される`() {
            fun localPrepare() {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")

                val insertUserSql =
                    "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
                val insertUserSqlParams =
                    MapSqlParameterSource().addValue("id", 1).addValue("email", "dummy@example.com")
                        .addValue("username", "dummy-username").addValue("password", "Passw0rd")
                        .addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertUserSql, insertUserSqlParams)

                val insertProfileSql =
                    "INSERT INTO profiles(id, user_id, bio, image, created_at, updated_at) VALUES (:id, :user_id, :bio, :image, :created_at, :updated_at);"
                val insertProfileSqlParams1 =
                    MapSqlParameterSource().addValue("id", 1).addValue("user_id", 1).addValue("bio", "dummy-bio")
                        .addValue("image", "dummy-image").addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertProfileSql, insertProfileSqlParams1)
                val insertProfileSqlParams2 =
                    MapSqlParameterSource().addValue("id", 2).addValue("user_id", 2).addValue("bio", "dummy-bio")
                        .addValue("image", "dummy-image").addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertProfileSql, insertProfileSqlParams2)
            }
            localPrepare()
            val confirmFollowingsSql =
                "SELECT COUNT(*) AS CNT FROM followings WHERE follower_id = :current_user_id AND following_id = :user_id"
            val confirmFollowingsParam = MapSqlParameterSource().addValue("user_id", 1).addValue("current_user_id", 2)

            /**
             * 実行前に挿入されていないことを確認
             */
            val beforeFollowingCount =
                namedParameterJdbcTemplate.queryForMap(confirmFollowingsSql, confirmFollowingsParam)["CNT"]
            assertThat(beforeFollowingCount).isEqualTo(0L)

            /**
             * 戻り値がフォロー済の OtherUser であることを確認
             */
            val expectedProfile = OtherUser.newWithoutValidation(
                UserId(1),
                Username.newWithoutValidation("dummy-username"),
                Bio.newWithoutValidation("dummy-bio"),
                Image.newWithoutValidation("dummy-image"),
                following = true
            )
            val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)
            when (val actual = profileRepository.follow(Username.newWithoutValidation("dummy-username"), UserId(2))) {
                is Left -> assert(false)
                is Right -> assertThat(actual.value).isEqualTo(expectedProfile)
            }

            /**
             * 実行後に1件だけ挿入されていることを確認
             */
            val afterResult = namedParameterJdbcTemplate.queryForList(confirmFollowingsSql, confirmFollowingsParam)
            assertThat(afterResult[0]["CNT"]).isEqualTo(1L)
        }

        @Test
        fun `正常系-フォロー済、戻り値がProfile-followings テーブルに挿入されない`() {
            fun localPrepare() {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")

                val insertUserSql =
                    "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
                val insertUserSqlParams =
                    MapSqlParameterSource().addValue("id", 1).addValue("email", "dummy@example.com")
                        .addValue("username", "dummy-username").addValue("password", "Passw0rd")
                        .addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertUserSql, insertUserSqlParams)

                val insertProfileSql =
                    "INSERT INTO profiles(id, user_id, bio, image, created_at, updated_at) VALUES (:id, :user_id, :bio, :image, :created_at, :updated_at);"
                val insertProfileSqlParams1 =
                    MapSqlParameterSource().addValue("id", 1).addValue("user_id", 1).addValue("bio", "dummy-bio")
                        .addValue("image", "dummy-image").addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertProfileSql, insertProfileSqlParams1)
                val insertProfileSqlParams2 =
                    MapSqlParameterSource().addValue("id", 2).addValue("user_id", 2).addValue("bio", "dummy-bio")
                        .addValue("image", "dummy-image").addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertProfileSql, insertProfileSqlParams2)

                // followings テーブルにすでにフォロー済のレコードを追加
                val sql2 =
                    "INSERT INTO followings(id, following_id, follower_id, created_at) VALUES (:id, :following_id, :follower_id, :created_at);"
                val sqlParams2 =
                    MapSqlParameterSource().addValue("id", 1).addValue("following_id", 1).addValue("follower_id", 2)
                        .addValue("created_at", date)
                namedParameterJdbcTemplate.update(sql2, sqlParams2)
            }
            localPrepare()
            val confirmFollowingsSql =
                "SELECT COUNT(*) AS CNT FROM followings WHERE follower_id = :current_user_id AND following_id = :user_id"
            val confirmFollowingsParam = MapSqlParameterSource().addValue("user_id", 1).addValue("current_user_id", 2)

            /**
             * 実行前に既に挿入されていることを確認
             */
            val beforeResult = namedParameterJdbcTemplate.queryForList(confirmFollowingsSql, confirmFollowingsParam)
            assertThat(beforeResult[0]["CNT"]).isEqualTo(1L)

            /**
             * 戻り値がフォロー済の OtherUser であることを確認
             */
            val expectedProfile = OtherUser.newWithoutValidation(
                UserId(1),
                Username.newWithoutValidation("dummy-username"),
                Bio.newWithoutValidation("dummy-bio"),
                Image.newWithoutValidation("dummy-image"),
                following = true
            )
            val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)
            when (val actual = profileRepository.follow(Username.newWithoutValidation("dummy-username"), UserId(2))) {
                is Left -> assert(false)
                is Right -> assertThat(actual.value).isEqualTo(expectedProfile)
            }

            /**
             * 実行後に挿入されていないことを確認
             */
            val afterResult = namedParameterJdbcTemplate.queryForList(confirmFollowingsSql, confirmFollowingsParam)
            assertThat(afterResult[0]["CNT"]).isEqualTo(1L)
        }

        @Test
        fun `異常系、 戻り値が NotFoundProfileByUsername`() {
            val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)

            val expected = ProfileRepository.FollowError.NotFoundProfileByUsername(
                Username.newWithoutValidation("dummy-username"), UserId(2)
            )

            when (val actual = profileRepository.follow(Username.newWithoutValidation("dummy-username"), UserId(2))) {
                is Left -> assertThat(actual.value).isEqualTo(expected)
                is Right -> assert(false)
            }
        }

        @Test
        @Disabled
        fun `異常系、 UnexpectedError が戻り値`() {
            TODO()
        }
    }

    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class `Unfollow(他ユーザーをアンフォロー)` {
        @BeforeEach
        @AfterAll
        fun reset() {
            resetDb()
        }

        @Test
        fun `正常系-フォロー済、 戻り値が OtherUser-followings テーブルから削除される`() {
            fun localPrepare() {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")

                val insertUserSql =
                    "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
                val insertUserSqlParams =
                    MapSqlParameterSource().addValue("id", 1).addValue("email", "dummy@example.com")
                        .addValue("username", "dummy-username").addValue("password", "Passw0rd")
                        .addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertUserSql, insertUserSqlParams)

                val insertProfileSql =
                    "INSERT INTO profiles(id, user_id, bio, image, created_at, updated_at) VALUES (:id, :user_id, :bio, :image, :created_at, :updated_at);"
                val insertProfileSqlParams1 =
                    MapSqlParameterSource().addValue("id", 1).addValue("user_id", 1).addValue("bio", "dummy-bio")
                        .addValue("image", "dummy-image").addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertProfileSql, insertProfileSqlParams1)
                val insertProfileSqlParams2 =
                    MapSqlParameterSource().addValue("id", 2).addValue("user_id", 2).addValue("bio", "dummy-bio")
                        .addValue("image", "dummy-image").addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertProfileSql, insertProfileSqlParams2)

                /**
                 * followings テーブルにすでにフォロー済のレコードを追加
                 */
                val insertFollowingsFollowerSql =
                    "INSERT INTO followings(id, following_id, follower_id, created_at) VALUES (:id, :following_id, :follower_id, :created_at);"
                val insertFollowingsFollowerSqlParams =
                    MapSqlParameterSource().addValue("id", 1).addValue("following_id", 1).addValue("follower_id", 2)
                        .addValue("created_at", date)
                namedParameterJdbcTemplate.update(insertFollowingsFollowerSql, insertFollowingsFollowerSqlParams)
                /**
                 * 他の follower_id が削除されないか確認のため他のフォロワーを追加
                 */
                val insertFollowingsOtherFollowerSqlParams =
                    MapSqlParameterSource().addValue("id", 2).addValue("following_id", 1).addValue("follower_id", 3)
                        .addValue("created_at", date)
                namedParameterJdbcTemplate.update(insertFollowingsFollowerSql, insertFollowingsOtherFollowerSqlParams)
            }
            localPrepare()
            val confirmFollowingsSql =
                "SELECT COUNT(*) AS CNT FROM followings WHERE follower_id = :current_user_id AND following_id = :user_id"
            val confirmFollowingsParam = MapSqlParameterSource().addValue("user_id", 1).addValue("current_user_id", 2)

            /**
             * 実行前に1件存在していることを確認
             */
            val beforeResult = namedParameterJdbcTemplate.queryForList(confirmFollowingsSql, confirmFollowingsParam)
            assertThat(beforeResult[0]["CNT"]).isEqualTo(1L)

            /**
             * 戻り値が未フォローの OtherUser であることを確認
             */
            val expectedProfile = OtherUser.newWithoutValidation(
                UserId(1),
                Username.newWithoutValidation("dummy-username"),
                Bio.newWithoutValidation("dummy-bio"),
                Image.newWithoutValidation("dummy-image"),
                following = false
            )
            val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)
            when (val actual = profileRepository.unfollow(Username.newWithoutValidation("dummy-username"), UserId(2))) {
                is Left -> assert(false)
                is Right -> assertThat(actual.value).isEqualTo(expectedProfile)
            }

            /**
             * 実行後に削除されていることを確認
             */
            val afterResult = namedParameterJdbcTemplate.queryForList(confirmFollowingsSql, confirmFollowingsParam)
            assertThat(afterResult[0]["CNT"]).isEqualTo(0L)

            /**
             * 余分な行が削除されていないか確認。オリジナル 2 行 -> 実行後 1 行
             */
            val confirmAllFollowingsSql = "SELECT COUNT(*) AS CNT FROM followings WHERE following_id = :user_id"
            val confirmAllFollowingsParam = MapSqlParameterSource().addValue("user_id", 1)
            val confirmAllFollowingsResult =
                namedParameterJdbcTemplate.queryForList(confirmAllFollowingsSql, confirmAllFollowingsParam)
            assertThat(confirmAllFollowingsResult[0]["CNT"]).isEqualTo(1L)
        }

        @Test
        fun `未フォロー、戻り値が OtherUser-followingsテーブルに変化なし`() {
            fun localPrepare() {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")

                val insertUserSql =
                    "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
                val insertUserSqlParams =
                    MapSqlParameterSource().addValue("id", 1).addValue("email", "dummy@example.com")
                        .addValue("username", "dummy-username").addValue("password", "Passw0rd")
                        .addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertUserSql, insertUserSqlParams)

                val insertProfileSql =
                    "INSERT INTO profiles(id, user_id, bio, image, created_at, updated_at) VALUES (:id, :user_id, :bio, :image, :created_at, :updated_at);"
                val insertProfileSqlParams =
                    MapSqlParameterSource().addValue("id", 1).addValue("user_id", 1).addValue("bio", "dummy-bio")
                        .addValue("image", "dummy-image").addValue("created_at", date).addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertProfileSql, insertProfileSqlParams)
            }
            localPrepare()

            /**
             * 戻り値が未フォローの OtherUser であることを確認
             */
            val expectedProfile = OtherUser.newWithoutValidation(
                UserId(1),
                Username.newWithoutValidation("dummy-username"),
                Bio.newWithoutValidation("dummy-bio"),
                Image.newWithoutValidation("dummy-image"),
                following = false
            )
            val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)
            when (val actual = profileRepository.unfollow(Username.newWithoutValidation("dummy-username"), UserId(2))) {
                is Left -> assert(false)
                is Right -> assertThat(actual.value).isEqualTo(expectedProfile)
            }
        }

        @Test
        fun `異常系 NotFoundProfileByUsername が戻り値`() {
            val profileRepository = ProfileRepositoryImpl(namedParameterJdbcTemplate)

            val expected = ProfileRepository.UnfollowError.NotFoundProfileByUsername(
                Username.newWithoutValidation("dummy-username"), UserId(2)
            )

            when (val actual = profileRepository.unfollow(Username.newWithoutValidation("dummy-username"), UserId(2))) {
                is Left -> assertThat(actual.value).isEqualTo(expected)
                is Right -> assert(false)
            }
        }

        @Test
        @Disabled
        fun `異常系 UnexpectedError が戻り値`() {
            TODO()
        }
    }

    @Nested
    @Tag("WithLocalDb")
    @DBRider
    class FindByUsername {
        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
            ]
        )
        fun `正常系-存在するユーザー名で検索した場合、該当する他ユーザーが戻り値`() {
            /**
             * given:
             */
            val profileRepository = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val existedUsername = Username.newWithoutValidation("paul-graham")

            /**
             * when:
             */
            val actual = profileRepository.findByUsername(existedUsername)

            /**
             * then:
             * - 中身チェック
             */
            when (actual) {
                is Left -> assert(false) { "原因: ${actual.value}" }
                is Right -> {
                    val user = actual.value
                    assertThat(user.username).isEqualTo(existedUsername)
                    assertThat(user.following).isFalse
                    assertThat(user.bio.value).isEqualTo("Lisper")
                    assertThat(user.image.value).isEqualTo("")
                }
            }
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
            ]
        )
        fun `正常系-あるユーザーがフォロー中のユーザー名で検索した場合、該当するフォロー中の他ユーザーが戻り値`() {
            /**
             * given:
             * - 存在するユーザー名
             * - 視点となるユーザーId
             */
            val profileRepository = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val existedUsername = Username.newWithoutValidation("paul-graham")
            val viewpointUserId = UserId(2).toOption()

            /**
             * when:
             */
            val actual = profileRepository.findByUsername(existedUsername, viewpointUserId)

            /**
             * then:
             */
            when (actual) {
                is Left -> assert(false) { "原因: ${actual.value}" }
                is Right -> {
                    val user = actual.value
                    assertThat(user.username).isEqualTo(existedUsername)
                    assertThat(user.following).isTrue
                    assertThat(user.bio.value).isEqualTo("Lisper")
                    assertThat(user.image.value).isEqualTo("")
                }
            }
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
            ]
        )
        fun `正常系-あるユーザーが未フォローのユーザー名で検索した場合、該当する未フォローの他ユーザーが戻り値`() {
            /**
             * given:
             * - 存在するユーザー名
             * - 視点となるユーザーId
             */
            val profileRepository = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val existedUsername = Username.newWithoutValidation("松本行弘")
            val viewpointUserId = UserId(1).toOption()

            /**
             * when:
             */
            val actual = profileRepository.findByUsername(existedUsername, viewpointUserId)

            /**
             * then:
             */
            when (actual) {
                is Left -> assert(false) { "原因: ${actual.value}" }
                is Right -> {
                    val user = actual.value
                    assertThat(user.username).isEqualTo(existedUsername)
                    assertThat(user.following).isFalse
                    assertThat(user.bio.value).isEqualTo("Rubyを作った")
                    assertThat(user.image.value).isEqualTo("")
                }
            }
        }

        @Test
        fun `準正常系-存在しないユーザー名で検索した場合、見つからなかった旨のエラーが戻り値`() {
            /**
             * given:
             */
            val profileRepository = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val notExistedUsername = Username.newWithoutValidation("名無しの権兵衛1")

            /**
             * when:
             * - 誰の視点からでもないユーザー名検索
             */
            val actual = profileRepository.findByUsername(notExistedUsername)

            /**
             * then:
             */
            val expected = ProfileRepository.FindByUsernameError.NotFound(notExistedUsername).left()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `準正常系-ある登録済みユーザー視点で存在しないユーザー名で検索した場合、見つからなかった旨のエラーが戻り値`() {
            /**
             * given:
             */
            val profileRepository = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val notExistedUsername = Username.newWithoutValidation("名無しの権兵衛2")
            val viewpointUserId = UserId(1).toOption()

            /**
             * when:
             * - あるユーザー視点から見たユーザー名検索
             */
            val actual = profileRepository.findByUsername(notExistedUsername, viewpointUserId)

            /**
             * then:
             */
            val expected = ProfileRepository.FindByUsernameError.NotFound(notExistedUsername).left()
            assertThat(actual).isEqualTo(expected)
        }

        /**
         * ユースケース上はありえない
         * 技術詳細(引数)的には可能なためテストを記述
         */
        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
            ]
        )
        fun `仕様外-存在しないユーザー視点から存在するユーザー名を検索しても例外は起きない`() {
            /**
             * given:
             */
            val profileRepository = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val existedUsername = Username.newWithoutValidation("paul-graham")
            val notExistedViewpointUserId = UserId(-1).toOption()

            /**
             * when:
             * - 存在しないユーザー視点から存在するユーザー名で検索
             */
            val actual = profileRepository.findByUsername(existedUsername, notExistedViewpointUserId)

            /**
             * then:
             * - 例外は起きない-見つかるが、未フォロー状態の他ユーザーが戻り値
             */
            when (actual) {
                is Left -> assert(false) { "原因: ${actual.value}" }
                is Right -> {
                    val user = actual.value
                    assertThat(user.username).isEqualTo(existedUsername)
                    assertThat(user.following).isFalse
                    assertThat(user.bio.value).isEqualTo("Lisper")
                    assertThat(user.image.value).isEqualTo("")
                }
            }
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
            ]
        )
        fun `仕様外-存在しないユーザー視点から存在しないユーザー名を検索しても例外は起きない`() {
            /**
             * given:
             */
            val profileRepository = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val notExistedUsername = Username.newWithoutValidation("名無しの権兵衛")
            val notExistedViewpointUserId = UserId(-1).toOption()

            /**
             * when:
             * - 存在しないユーザー視点から見た存在しないユーザー名で検索
             */
            val actual = profileRepository.findByUsername(notExistedUsername, notExistedViewpointUserId)

            /**
             * then:
             * - 例外は起きない-見つからなかった旨のエラーが戻り値
             */
            val expected = ProfileRepository.FindByUsernameError.NotFound(notExistedUsername).left()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @Tag("WithLocalDb")
    @DBRider
    class FilterByUserIds {

        private class TestCase(
            val title: String,
            val userIdSet: Set<UserId>,
            val viewpointUserId: Option<UserId> = none(),
            val expected: Either<Nothing, Set<OtherUser>>
        )

        @TestFactory
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
            ]
        )
        fun test(): Stream<DynamicNode> = Stream.of(
            TestCase(
                title = "正常系-存在する登録済みユーザーID郡でフィルタをした場合、それに該当する他ユーザー郡が戻り値",
                userIdSet = setOf(
                    UserId(1)
                ),
                expected = setOf(
                    OtherUser.newWithoutValidation(
                        userId = UserId(1),
                        username = Username.newWithoutValidation("paul-graham"),
                        bio = Bio.newWithoutValidation("Lisper"),
                        image = Image.newWithoutValidation(""),
                        following = false
                    )
                ).right()
            ),
            TestCase(
                title = "正常系-存在する登録済みユーザーID郡でフィルタをした場合、それに該当する他ユーザー郡が戻り値",
                userIdSet = setOf(
                    UserId(1),
                    UserId(3),
                ),
                expected = setOf(
                    OtherUser.newWithoutValidation(
                        userId = UserId(1),
                        username = Username.newWithoutValidation("paul-graham"),
                        bio = Bio.newWithoutValidation("Lisper"),
                        image = Image.newWithoutValidation(""),
                        following = false
                    ),
                    OtherUser.newWithoutValidation(
                        userId = UserId(3),
                        username = Username.newWithoutValidation("graydon-hoare"),
                        bio = Bio.newWithoutValidation("Rustを作った"),
                        image = Image.newWithoutValidation(""),
                        following = false
                    )
                ).right()
            ),
            TestCase(
                title = "正常系-登録済みユーザーID郡が空でフィルタをした場合、空の他ユーザー郡が戻り値",
                userIdSet = emptySet(),
                expected = emptySet<OtherUser>().right()
            ),
            TestCase(
                title = "正常系-ある特定ユーザー視点-存在する登録済みユーザーID郡でフィルタをした場合、それに該当するフォロー状態が表現された他ユーザー郡が戻り値",
                userIdSet = setOf(
                    UserId(1),
                    UserId(2),
                    UserId(3),
                ),
                viewpointUserId = UserId(2).toOption(),
                expected = setOf(
                    OtherUser.newWithoutValidation(
                        userId = UserId(1),
                        username = Username.newWithoutValidation("paul-graham"),
                        bio = Bio.newWithoutValidation("Lisper"),
                        image = Image.newWithoutValidation(""),
                        following = true
                    ),
                    OtherUser.newWithoutValidation(
                        userId = UserId(2),
                        username = Username.newWithoutValidation("松本行弘"),
                        bio = Bio.newWithoutValidation("Rubyを作った"),
                        image = Image.newWithoutValidation(""),
                        following = false
                    ),
                    OtherUser.newWithoutValidation(
                        userId = UserId(3),
                        username = Username.newWithoutValidation("graydon-hoare"),
                        bio = Bio.newWithoutValidation("Rustを作った"),
                        image = Image.newWithoutValidation(""),
                        following = false
                    ),
                ).right()
            ),
        ).map { testCase ->
            dynamicTest(testCase.title) {
                /**
                 * given:
                 */
                val profileRepository = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
                val userIdSet = testCase.userIdSet
                val viewpointUserId = testCase.viewpointUserId

                /**
                 * when:
                 */
                val actual = profileRepository.filterByUserIds(userIdSet, viewpointUserId)

                /**
                 * then:
                 */
                assertThat(actual).isEqualTo(testCase.expected)
            }
        }

        /**
         * ユースケース上はありえない
         * 技術詳細(引数)的には可能なためテストを記述
         */
        @TestFactory
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
            ]
        )
        fun testOutOfSpecification(): Stream<DynamicNode> = Stream.of(
            TestCase(
                title = "仕様外-フィルタ対象が存在する登録済みユーザーIDと存在しないユーザーIDが混ざっていた場合、例外は起きず、それに該当する他ユーザー郡のみが戻り値",
                userIdSet = setOf(
                    UserId(1),
                    UserId(-1),
                    UserId(-2),
                ),
                expected = setOf(
                    OtherUser.newWithoutValidation(
                        userId = UserId(1),
                        username = Username.newWithoutValidation("paul-graham"),
                        bio = Bio.newWithoutValidation("Lisper"),
                        image = Image.newWithoutValidation(""),
                        following = false
                    )
                ).right()
            ),
            TestCase(
                title = "仕様外-フィルタ対象が存在しないユーザーID郡だった場合、例外は起きず、空の他ユーザー郡のみが戻り値",
                userIdSet = setOf(
                    UserId(-1),
                    UserId(-2),
                    UserId(-3),
                ),
                expected = emptySet<OtherUser>().right()
            ),
        ).map { testCase ->
            dynamicTest(testCase.title) {
                /**
                 * given:
                 */
                val profileRepository = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
                val userIdSet = testCase.userIdSet
                val viewpointUserId = testCase.viewpointUserId

                /**
                 * when:
                 */
                val actual = profileRepository.filterByUserIds(userIdSet, viewpointUserId)

                /**
                 * then:
                 */
                assertThat(actual).isEqualTo(testCase.expected)
            }
        }
    }

    @Tag("WithLocalDb")
    @DBRider
    class FilterFavoritedByUser {
        private class TestCase(
            val title: String,
            val viewpointUserId: UserId,
            val expected: Set<OtherUser>
        )

        @TestFactory
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
            ]
        )
        fun test(): Stream<DynamicNode> = Stream.of(
            TestCase(
                title = "正常系-フォローしている人全員分を取得できる",
                viewpointUserId = UserId(2),
                expected = SeedData.otherUsersFromViewpointSet()[UserId(2)]!!.filter { it.following }.toSet()
            ),
            TestCase(
                title = "正常系-誰もフォローしていない場合は、空のSetが戻り値",
                viewpointUserId = UserId(1),
                expected = emptySet()
            )
        ).map { testCase ->
            dynamicTest(testCase.title) {
                /**
                 * given:
                 */
                val profileRepository = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)

                /**
                 * when:
                 */
                val actual = profileRepository.filterFollowedByUser(testCase.viewpointUserId)

                /**
                 * then:
                 * - 数が一致
                 * - それぞれのOtherUserの中身が同じ
                 */
                when (actual) {
                    is Left -> assert(false) { "原因: ${actual.value}" }
                    is Right -> {
                        val followedOtherUsers = actual.value
                        assertThat(followedOtherUsers)
                            .`as`("${testCase.viewpointUserId}がフォローしている人数が一致する")
                            .isEqualTo(testCase.expected)
                        followedOtherUsers.forEach { followedOtherUser ->
                            val expectedOtherUser = testCase.expected.find { it.userId == followedOtherUser.userId }!!
                            assertThat(followedOtherUser.username).isEqualTo(expectedOtherUser.username)
                            assertThat(followedOtherUser.bio).isEqualTo(expectedOtherUser.bio)
                            assertThat(followedOtherUser.image).isEqualTo(expectedOtherUser.image)
                            assertThat(followedOtherUser.following).isEqualTo(expectedOtherUser.following)
                        }
                    }
                }
            }
        }
    }
}
