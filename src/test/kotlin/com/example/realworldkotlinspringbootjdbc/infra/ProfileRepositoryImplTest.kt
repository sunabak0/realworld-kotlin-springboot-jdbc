package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.None
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
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("WithLocalDb")
class ProfileRepositoryImplTest {
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("Show(プロフィールを表示)")
    class Show {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(value = ["datasets/yml/given/users.yml"])
        fun `正常系-未ログインで、username で指定した登録済ユーザーが存在する場合、Profile を取得できる`() {
            /**
             * given:
             */
            val profileRepository = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val username = Username.newWithoutValidation("paul-graham")

            /**
             * when:
             */
            val actual = profileRepository.show(username = username)

            /**
             * then:
             * - following = false（必ず未フォロー）
             */
            val expected = OtherUser.newWithoutValidation(
                userId = UserId(1),
                username = Username.newWithoutValidation("paul-graham"),
                bio = Bio.newWithoutValidation("Lisper"),
                image = Image.newWithoutValidation(""),
                following = false
            )
            when (actual) {
                is Left -> assert(false)
                is Right -> {
                    assertThat(actual.value.userId).isEqualTo(expected.userId)
                    assertThat(actual.value.username).isEqualTo(expected.username)
                    assertThat(actual.value.bio).isEqualTo(expected.bio)
                    assertThat(actual.value.following).isEqualTo(expected.following)
                }
            }
        }

        @Test
        @DataSet(value = ["datasets/yml/given/users.yml"])
        fun `正常系-ログイン済で、username で指定した登録済ユーザーが存在し、フォロー済の場合、フォロー状態の Profileを取得できる`() {
            /**
             * given:
             */
            val profileRepository = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val username = Username.newWithoutValidation("paul-graham")
            val currentUserId = UserId(2)

            /**
             * when:
             */
            val actual = profileRepository.show(username = username, currentUserId = currentUserId.toOption())

            /**
             * then:
             * - following = true
             */
            val expected = OtherUser.newWithoutValidation(
                userId = UserId(1),
                username = Username.newWithoutValidation("paul-graham"),
                bio = Bio.newWithoutValidation("Lisper"),
                image = Image.newWithoutValidation(""),
                following = true
            )
            when (actual) {
                is Left -> assert(false)
                is Right -> {
                    assertThat(actual.value.userId).isEqualTo(expected.userId)
                    assertThat(actual.value.username).isEqualTo(expected.username)
                    assertThat(actual.value.bio).isEqualTo(expected.bio)
                    assertThat(actual.value.following).isEqualTo(expected.following)
                }
            }
        }

        @Test
        @DataSet(value = ["datasets/yml/given/users.yml"])
        fun `正常系-ログイン済で、username で指定した登録済ユーザーが存在し、未フォローの場合、未フォロー状態の Profileを取得できる`() {
            /**
             * given:
             * - 存在する username
             * - username をフォローしていないユーザー ID
             */
            val profileRepository = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val username = Username.newWithoutValidation("graydon-hoare")
            val currentUserId = UserId(1)

            /**
             * when:
             */
            val actual = profileRepository.show(username = username, currentUserId = currentUserId.toOption())

            /**
             * then:
             * - following = false
             */
            val expected = OtherUser.newWithoutValidation(
                userId = UserId(3),
                username = Username.newWithoutValidation("graydon-hoare"),
                bio = Bio.newWithoutValidation("Rustを作った"),
                image = Image.newWithoutValidation(""),
                following = false
            )
            when (actual) {
                is Left -> assert(false)
                is Right -> {
                    assertThat(actual.value.userId).isEqualTo(expected.userId)
                    assertThat(actual.value.username).isEqualTo(expected.username)
                    assertThat(actual.value.bio).isEqualTo(expected.bio)
                    assertThat(actual.value.following).isEqualTo(expected.following)
                }
            }
        }

        @Test
        @DataSet(value = ["datasets/yml/given/users.yml"])
        fun `準正常系-ログイン済で、username で指定した登録済ユーザーが存在しない場合、NotFoundProfileByUsername が戻り値`() {
            /**
             * given:
             * - 存在しない username
             * - ユーザー ID
             */
            val profileRepository = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val username = Username.newWithoutValidation("dummy-username")
            val currentUserId = UserId(1)

            /**
             * when:
             */
            val actual = profileRepository.show(username = username, currentUserId = currentUserId.toOption())

            /**
             * then:
             */
            val expected = ProfileRepository.ShowError.NotFoundProfileByUsername(
                username, currentUserId.toOption()
            )
            when (actual) {
                is Left -> assertThat(actual.value).isEqualTo(expected)
                is Right -> assert(false)
            }
        }

        @Test
        @DataSet(value = ["datasets/yml/given/users.yml"])
        fun `準正常系-未ログインで、username で指定した登録済ユーザーが存在しない場合、NotFoundProfileByUsername が戻り値`() {
            /**
             * given:
             * - 存在しない username
             */
            val profileRepository = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val username = Username.newWithoutValidation("dummy-username")

            /**
             * when:
             */
            val actual = profileRepository.show(username = username)

            /**
             * then:
             */
            val expected = ProfileRepository.ShowError.NotFoundProfileByUsername(username, None)
            when (actual) {
                is Left -> assertThat(actual.value).isEqualTo(expected)
                is Right -> assert(false)
            }
        }
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("Follow（他ユーザーをフォロー）")
    class Follow {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(value = ["datasets/yml/given/users.yml"])
        @ExpectedDataSet(
            value = ["datasets/yml/then/profile_repository/follow-success.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        // NOTE: @ExportDataSetはgivenの@DataSetが変更された時用に残しておく
        // @ExportDataSet(
        //     format = DataSetFormat.YML,
        //     outputName = "src/test/resources/datasets/yml/then/profile_repository/follow-success.yml",
        //     includeTables = ["followings", "profiles", "users"]
        // )
        fun `正常系-username で指定したユーザーが存在し、まだフォローしていない場合、フォローする`() {
            /**
             * given:
             * - ProfileRepository
             * - 存在する username
             * - 存在する username をフォローしていない、UserId
             */
            val profileRepositoryImpl = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val username = Username.newWithoutValidation("松本行弘")
            val currentUserId = UserId(1)

            /**
             * when:
             */
            val actual = profileRepositoryImpl.follow(username = username, currentUserId = currentUserId)

            /**
             * then:
             */
            val expected = OtherUser.newWithoutValidation(
                userId = UserId(2),
                username = Username.newWithoutValidation("松本行弘"),
                bio = Bio.newWithoutValidation("Rubyを作った"),
                image = Image.newWithoutValidation(""),
                following = true
            )
            when (actual) {
                is Left -> assert(false)
                is Right -> {
                    assertThat(actual.value.userId).isEqualTo(expected.userId)
                    assertThat(actual.value.username).isEqualTo(expected.username)
                    assertThat(actual.value.bio).isEqualTo(expected.bio)
                    assertThat(actual.value.following).isEqualTo(expected.following)
                }
            }
        }

        @Test
        @DataSet(value = ["datasets/yml/given/users.yml"])
        @ExpectedDataSet(
            value = ["datasets/yml/given/users.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `正常系-username で指定したユーザーが存在し、フォロー済の場合、フォロー済のままである`() {
            /**
             * given:
             * - ProfileRepository
             * - 存在する username
             * - 存在する username をフォローしている、UserId
             */
            val profileRepositoryImpl = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val username = Username.newWithoutValidation("paul-graham")
            val currentUserId = UserId(3)

            /**
             * when:
             */
            val actual = profileRepositoryImpl.follow(username = username, currentUserId = currentUserId)

            /**
             * then:
             */
            val expected = OtherUser.newWithoutValidation(
                userId = UserId(1),
                username = Username.newWithoutValidation("paul-graham"),
                bio = Bio.newWithoutValidation("Lisper"),
                image = Image.newWithoutValidation(""),
                following = true
            )
            when (actual) {
                is Left -> assert(false)
                is Right -> {
                    assertThat(actual.value.userId).isEqualTo(expected.userId)
                    assertThat(actual.value.username).isEqualTo(expected.username)
                    assertThat(actual.value.bio).isEqualTo(expected.bio)
                    assertThat(actual.value.following).isEqualTo(expected.following)
                }
            }
        }

        @Test
        @DataSet(value = ["datasets/yml/given/users.yml"])
        @ExpectedDataSet(
            value = ["datasets/yml/given/users.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `準正常系-username に該当するユーザーが存在しない場合、戻り値が NotFoundProfileByUsername`() {
            /**
             * given:
             * - ProfileRepository
             * - 存在する username
             * - 有効な UserId （存在しない UserId でもエラーが発生しない）
             */
            val profileRepositoryImpl = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val username = Username.newWithoutValidation("dummy-username")
            val currentUserId = UserId(3)

            /**
             * when:
             */
            val actual = profileRepositoryImpl.follow(username = username, currentUserId = currentUserId)

            /**
             * then:
             */
            val expected = ProfileRepository.FollowError.NotFoundProfileByUsername(
                Username.newWithoutValidation("dummy-username"), UserId(3)
            )
            when (actual) {
                is Left -> assertThat(actual.value).isEqualTo(expected)
                is Right -> assert(false)
            }
        }
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("Unfollow（他ユーザーをアンフォロー）")
    class Unfollow {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(value = ["datasets/yml/given/users.yml"])
        @ExpectedDataSet(
            value = ["datasets/yml/then/profile_repository/unfollow-success.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        // NOTE: @ExportDataSetはgivenの@DataSetが変更された時用に残しておく
        // @ExportDataSet(
        //     format = DataSetFormat.YML,
        //     outputName = "src/test/resources/datasets/yml/then/profile_repository/unfollow-success.yml",
        //     includeTables = ["followings", "profiles", "users"]
        // )
        fun `正常系-username で指定したユーザーが存在し、フォロー済の場合、未フォローになる`() {
            /**
             * given:
             * - ProfileRepository
             * - 存在する username
             * - 存在する username をフォローしていない、UserId
             */
            val profileRepositoryImpl = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val username = Username.newWithoutValidation("paul-graham")
            val currentUserId = UserId(3)

            /**
             * when:
             */
            val actual = profileRepositoryImpl.unfollow(username = username, currentUserId = currentUserId)

            /**
             * then:
             */
            val expected = OtherUser.newWithoutValidation(
                userId = UserId(1),
                username = Username.newWithoutValidation("paul-graham"),
                bio = Bio.newWithoutValidation("Lisper"),
                image = Image.newWithoutValidation(""),
                following = false
            )
            when (actual) {
                is Left -> assert(false)
                is Right -> {
                    assertThat(actual.value.userId).isEqualTo(expected.userId)
                    assertThat(actual.value.username).isEqualTo(expected.username)
                    assertThat(actual.value.bio).isEqualTo(expected.bio)
                    assertThat(actual.value.following).isEqualTo(expected.following)
                }
            }
        }

        @Test
        @DataSet(value = ["datasets/yml/given/users.yml"])
        @ExpectedDataSet(
            value = ["datasets/yml/given/users.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `正常系-username で指定したユーザーが存在し、未フォローの場合、未フォローのままである`() {
            /**
             * given:
             * - ProfileRepository
             * - 存在する username
             * - 存在する username をフォローしていない、UserId
             */
            val profileRepositoryImpl = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val username = Username.newWithoutValidation("松本行弘")
            val currentUserId = UserId(1)

            /**
             * when:
             */
            val actual = profileRepositoryImpl.unfollow(username = username, currentUserId = currentUserId)

            /**
             * then:
             * - following = false（未フォロー）
             */
            val expected = OtherUser.newWithoutValidation(
                userId = UserId(2),
                username = Username.newWithoutValidation("松本行弘"),
                bio = Bio.newWithoutValidation("Rubyを作った"),
                image = Image.newWithoutValidation(""),
                following = false
            )
            when (actual) {
                is Left -> assert(false)
                is Right -> {
                    assertThat(actual.value.userId).isEqualTo(expected.userId)
                    assertThat(actual.value.username).isEqualTo(expected.username)
                    assertThat(actual.value.bio).isEqualTo(expected.bio)
                    assertThat(actual.value.following).isEqualTo(expected.following)
                }
            }
        }

        @Test
        @DataSet(value = ["datasets/yml/given/users.yml"])
        @ExpectedDataSet(
            value = ["datasets/yml/given/users.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `準正常系-username に該当するユーザーが存在しない場合、戻り値が NotFoundProfileByUsername`() {
            /**
             * given:
             * - ProfileRepository
             * - 存在する username
             * - 有効な UserId （存在しない UserId でもエラーが発生しない）
             */
            val profileRepositoryImpl = ProfileRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val username = Username.newWithoutValidation("dummy-username")
            val currentUserId = UserId(3)

            /**
             * when:
             */
            val actual = profileRepositoryImpl.unfollow(username = username, currentUserId = currentUserId)

            /**
             * then:
             */
            val expected = ProfileRepository.UnfollowError.NotFoundProfileByUsername(
                Username.newWithoutValidation("dummy-username"), UserId(3)
            )
            when (actual) {
                is Left -> assertThat(actual.value).isEqualTo(expected)
                is Right -> assert(false)
            }
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
