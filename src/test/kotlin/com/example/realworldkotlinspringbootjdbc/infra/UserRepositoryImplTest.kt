package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UnregisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UpdatableRegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.Password
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.infra.helper.SeedData
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Date

class UserRepositoryImplTest {
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class Register {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet("datasets/yml/given/users.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/then/user_repository/register-success.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        // NOTE: @ExportDataSetはgivenの@DataSetが変更用に残しておく
        // @ExportDataSet(format = DataSetFormat.YML, outputName = "src/test/resources/datasets/yml/then/user_repository/register-success.yml", includeTables = ["users", "profiles", "followings"])
        fun `成功-EmailとUsernameがまだ登録されていない未登録ユーザーは、登録できる`() {
            /**
             * given:
             */
            val userRepository = UserRepositoryImpl(
                namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate,
                jdbcDatabase = DbConnection.jdbcDatabase,
            )
            val user = object : UnregisteredUser {
                override val email: Email get() = Email.newWithoutValidation("unregistered@example.com")
                override val password: Password get() = Password.newWithoutValidation("Passw0rd")
                override val username: Username get() = Username.newWithoutValidation("unregistered-username")
            }

            /**
             * when:
             */
            val actual = userRepository.register(user)

            /**
             * then:
             */
            assertThat(actual.isRight()).isTrue
        }

        @Test
        @DataSet("datasets/yml/given/users.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/given/users.yml"],
            ignoreCols = ["created_at", "updated_at"]
        )
        fun `失敗-Emailが既に利用されていた場合、その旨のエラーが返り、登録できない`() {
            /**
             * given:
             */
            val userRepository = UserRepositoryImpl(
                namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate,
                jdbcDatabase = DbConnection.jdbcDatabase,
            )
            val user = object : UnregisteredUser {
                override val email: Email get() = Email.newWithoutValidation("paul-graham@example.com")
                override val password: Password get() = Password.newWithoutValidation("Passw0rd")
                override val username: Username get() = Username.newWithoutValidation("non-used-username")
            }

            /**
             * when:
             */
            val actual = userRepository.register(user)

            /**
             * then:
             */
            val expected = UserRepository.RegisterError.AlreadyRegisteredEmail(user.email).left()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet("datasets/yml/given/users.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/given/users.yml"],
            ignoreCols = ["created_at", "updated_at"]
        )
        fun `失敗-Usernameが既に利用されていた場合、その旨のエラーが返り、登録できない`() {
            /**
             * given:
             */
            val userRepository = UserRepositoryImpl(
                namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate,
                jdbcDatabase = DbConnection.jdbcDatabase,
            )
            val user = object : UnregisteredUser {
                override val email: Email get() = Email.newWithoutValidation("non-used-email@example.com")
                override val password: Password get() = Password.newWithoutValidation("Passw0rd")
                override val username: Username get() = Username.newWithoutValidation("paul-graham")
            }

            // when:
            val actual = userRepository.register(user)

            // then:
            val expected = UserRepository.RegisterError.AlreadyRegisteredUsername(user.username).left()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class FindByEmailWithPassword {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet("datasets/yml/given/users.yml")
        fun `成功-対象の登録済みユーザーが存在する場合、パスワード付きで登録済みユーザーが戻り値となる`() {
            /**
             * given:
             */
            val userRepository = UserRepositoryImpl(
                namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate,
                jdbcDatabase = DbConnection.jdbcDatabase,
            )
            val searchingEmail = Email.newWithoutValidation("paul-graham@example.com")

            /**
             * when:
             */
            val actual = userRepository.findByEmailWithPassword(searchingEmail)

            /**
             * then:
             */
            when (actual) {
                is Left -> assert(false)
                is Right -> {
                    val (foundUser, password) = actual.value
                    assertThat(foundUser.email.value).isEqualTo(searchingEmail.value)
                    assertThat(password.value).isEqualTo("Passw0rd")
                }
            }
        }

        @Test
        @DataSet("datasets/yml/given/empty-users.yml")
        fun `失敗-対象の登録済みユーザーが存在しない場合、その旨のエラーが戻り値となる`() {
            /**
             * given:
             */
            val userRepository = UserRepositoryImpl(
                namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate,
                jdbcDatabase = DbConnection.jdbcDatabase,
            )
            val searchingEmail = Email.newWithoutValidation("notfound@example.com")

            /**
             * when:
             */
            val actual = userRepository.findByEmailWithPassword(searchingEmail)

            /**
             * then:
             */
            val expected = UserRepository.FindByEmailWithPasswordError.NotFound(searchingEmail).left()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class FindByUserId {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet("datasets/yml/given/users.yml")
        fun `成功-対象の登録済みユーザーが存在する場合、登録済みユーザーが戻り値となる`() {
            /**
             * given:
             */
            val userRepository = UserRepositoryImpl(
                namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate,
                jdbcDatabase = DbConnection.jdbcDatabase,
            )
            val searchingUserId = UserId(1)

            /**
             * when:
             */
            val actual = userRepository.findByUserId(searchingUserId)

            /**
             * then:
             */
            val expected = RegisteredUser.newWithoutValidation(
                UserId(1),
                Email.newWithoutValidation("paul-graham@example.com"),
                Username.newWithoutValidation("paul-graham"),
                Bio.newWithoutValidation("Lisper"),
                Image.newWithoutValidation(""),
            ).right()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet("datasets/yml/given/empty-users.yml")
        fun `失敗-対象の登録済みユーザーが存在しない場合、その旨のエラーが戻り値となる`() {
            /**
             * given:
             */
            val userRepository = UserRepositoryImpl(
                namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate,
                jdbcDatabase = DbConnection.jdbcDatabase,
            )
            val searchingUserId = UserId(1)

            /**
             * when:
             */
            val actual = userRepository.findByUserId(searchingUserId)

            /**
             * then:
             */
            val expected = UserRepository.FindByUserIdError.NotFound(UserId(1)).left()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class Update {
        @BeforeEach
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet("datasets/yml/given/users.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/then/user_repository/update-success-case-of-unique-email.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        // NOTE: @ExportDataSetはgivenの@DataSetが変更用に残しておく
        // @ExportDataSet(
        //     format = DataSetFormat.YML,
        //     outputName = "src/test/resources/datasets/yml/then/user_repository/update-success-case-of-unique-email.yml",
        //     includeTables = ["users", "profiles"]
        // )
        fun `正常系-emailが誰にも使われていない場合、更新が反映された登録済みユーザーが戻り値`() {
            /**
             * given:
             * - (存在する)emailだけ更新する更新可能な登録済みユーザー
             */
            val userRepository = UserRepositoryImpl(
                namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate,
                jdbcDatabase = DbConnection.jdbcDatabase,
            )
            val existedRegisteredUser = SeedData.users().minByOrNull { it.userId.value }!!
            val updatableRegisteredUser = object : UpdatableRegisteredUser {
                override val userId: UserId get() = existedRegisteredUser.userId
                override val email: Email get() = Email.newWithoutValidation("new+${existedRegisteredUser.email.value}")
                override val username: Username get() = existedRegisteredUser.username
                override val bio: Bio get() = existedRegisteredUser.bio
                override val image: Image get() = existedRegisteredUser.image
                override val updatedAt: Date get() = Date()
            }

            /**
             * when:
             */
            val actual = userRepository.update(updatableRegisteredUser)

            /**
             * then:
             * - 詰め替えができているか
             */
            val expected = Unit.right()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet("datasets/yml/given/users.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/then/user_repository/update-success-case-of-unique-username.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        // NOTE: @ExportDataSetはgivenの@DataSetが変更用に残しておく
        // @ExportDataSet(
        //     format = DataSetFormat.YML,
        //     outputName = "src/test/resources/datasets/yml/then/user_repository/update-success-case-of-unique-username.yml",
        //     includeTables = ["users", "profiles"]
        // )
        fun `正常系-usernameが誰にも使われていない場合、更新が反映された登録済みユーザーが戻り値`() {
            /**
             * given:
             * - (存在する)usernameだけ更新する更新可能な登録済みユーザー
             */
            val userRepository = UserRepositoryImpl(
                namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate,
                jdbcDatabase = DbConnection.jdbcDatabase,
            )
            val existedRegisteredUser = SeedData.users().minByOrNull { it.userId.value }!!
            val updatableRegisteredUser = object : UpdatableRegisteredUser {
                override val userId: UserId get() = existedRegisteredUser.userId
                override val email: Email get() = existedRegisteredUser.email
                override val username: Username get() = Username.newWithoutValidation("new+${existedRegisteredUser.username.value}")
                override val bio: Bio get() = existedRegisteredUser.bio
                override val image: Image get() = existedRegisteredUser.image
                override val updatedAt: Date get() = Date()
            }

            /**
             * when:
             */
            val actual = userRepository.update(updatableRegisteredUser)

            /**
             * then:
             */
            val expected = Unit.right()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet("datasets/yml/given/users.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/then/user_repository/update-success-case-of-all-properties.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        // NOTE: @ExportDataSetはgivenの@DataSetが変更用に残しておく
        // @ExportDataSet(
        //     format = DataSetFormat.YML,
        //     outputName = "src/test/resources/datasets/yml/then/user_repository/update-success-case-of-all-properties.yml",
        //     includeTables = ["users", "profiles"]
        // )
        fun `正常系-emailもusernameも誰にも使われていない場合、全てのプロパティの更新が反映された登録済みユーザーが戻り値`() {
            /**
             * given:
             * - (存在する)全て更新する更新可能な登録済みユーザー
             */
            val userRepository = UserRepositoryImpl(
                namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate,
                jdbcDatabase = DbConnection.jdbcDatabase,
            )
            val existedRegisteredUser = SeedData.users().minByOrNull { it.userId.value }!!
            val updatableRegisteredUser = object : UpdatableRegisteredUser {
                override val userId: UserId get() = existedRegisteredUser.userId
                override val email: Email get() = Email.newWithoutValidation("new+${existedRegisteredUser.email.value}")
                override val username: Username get() = Username.newWithoutValidation("new+${existedRegisteredUser.username.value}")
                override val bio: Bio get() = Bio.newWithoutValidation("new+${existedRegisteredUser.bio.value}")
                override val image: Image get() = Image.newWithoutValidation("new+${existedRegisteredUser.image.value}")
                override val updatedAt: Date get() = Date()
            }

            /**
             * when:
             */
            val actual = userRepository.update(updatableRegisteredUser)

            /**
             * then:
             */
            val expected = Unit.right()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet("datasets/yml/given/users.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/given/users.yml"],
            ignoreCols = ["created_at", "updated_at"]
        )
        fun `準正常系-emailが自分以外の誰かに利用されていた場合、その旨のエラーが戻り値となり、DBは更新されない`() {
            /**
             * given:
             * - emailだけ更新しようとしている更新可能な登録済みユーザー
             */
            val userRepository = UserRepositoryImpl(
                namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate,
                jdbcDatabase = DbConnection.jdbcDatabase,
            )
            val existedUsers = SeedData.users().sortedBy { it.userId.value }
            val alreadyUsedEmailUpdatableRegisteredUser = object : UpdatableRegisteredUser {
                override val userId: UserId get() = existedUsers[0].userId
                override val email: Email get() = existedUsers[1].email // 他の人が利用しているemail
                override val username: Username get() = existedUsers[0].username
                override val bio: Bio get() = existedUsers[0].bio
                override val image: Image get() = existedUsers[0].image
                override val updatedAt: Date get() = Date()
            }

            /**
             * when:
             */
            val actual = userRepository.update(alreadyUsedEmailUpdatableRegisteredUser)

            /**
             * then:
             */
            val expected = UserRepository.UpdateError.AlreadyRegisteredEmail(alreadyUsedEmailUpdatableRegisteredUser.email).left()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet("datasets/yml/given/users.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/given/users.yml"],
            ignoreCols = ["created_at", "updated_at"]
        )
        fun `準正常系-usernameが自分以外の誰かに利用されていた場合、その旨のエラーが戻り値となり、DBは更新されない`() {
            /**
             * given:
             * - usernameだけ更新しようとしている更新可能な登録済みユーザー
             */
            val userRepository = UserRepositoryImpl(
                namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate,
                jdbcDatabase = DbConnection.jdbcDatabase,
            )
            val existedUsers = SeedData.users().sortedBy { it.userId.value }
            val alreadyUsedUsernameUpdatableRegisteredUser = object : UpdatableRegisteredUser {
                override val userId: UserId get() = existedUsers[0].userId
                override val email: Email get() = existedUsers[0].email
                override val username: Username get() = existedUsers[1].username // 他の人が利用しているusername
                override val bio: Bio get() = existedUsers[0].bio
                override val image: Image get() = existedUsers[0].image
                override val updatedAt: Date get() = Date()
            }

            /**
             * when:
             */
            val actual = userRepository.update(alreadyUsedUsernameUpdatableRegisteredUser)

            /**
             * then:
             */
            val expected = UserRepository.UpdateError.AlreadyRegisteredUsername(alreadyUsedUsernameUpdatableRegisteredUser.username).left()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet("datasets/yml/given/empty-users.yml")
        @ExpectedDataSet(value = ["datasets/yml/given/empty-users.yml"])
        fun `準正常系-指定した登録済みユーザーが存在しない場合、その旨のエラーが戻り値となり、DBは更新されない`() {
            /**
             * given:
             * - 存在しないユーザー
             */
            val userRepository = UserRepositoryImpl(
                namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate,
                jdbcDatabase = DbConnection.jdbcDatabase,
            )
            val notExistedUpdatableRegisteredUser = object : UpdatableRegisteredUser {
                override val userId: UserId get() = UserId(SeedData.users().size + 1)
                override val email: Email get() = Email.newWithoutValidation("fake@example.com")
                override val username: Username get() = Username.newWithoutValidation("fake-username")
                override val bio: Bio get() = Bio.newWithoutValidation("fake-bio")
                override val image: Image get() = Image.newWithoutValidation("fake-image")
                override val updatedAt: Date get() = Date()
            }

            /**
             * when:
             */
            val actual = userRepository.update(notExistedUpdatableRegisteredUser)

            /**
             * then:
             */
            val expected = UserRepository.UpdateError.NotFound(notExistedUpdatableRegisteredUser.userId).left()
            assertThat(actual).isEqualTo(expected)
        }
    }
}
