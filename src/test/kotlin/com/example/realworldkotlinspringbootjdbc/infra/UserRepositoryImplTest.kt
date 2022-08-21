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
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

class UserRepositoryImplTest {
    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @SpringBootTest
    @DBRider
    @DisplayName("ユーザー登録")
    class RegisterTest(@Autowired val userRepository: UserRepository) {
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
            // given:
            val user = object : UnregisteredUser {
                override val email: Email get() = Email.newWithoutValidation("unregistered@example.com")
                override val password: Password get() = Password.newWithoutValidation("Passw0rd")
                override val username: Username get() = Username.newWithoutValidation("unregistered-username")
            }

            // when:
            val actual = userRepository.register(user)

            // then:
            assertThat(actual.isRight()).isTrue
        }

        @Test
        @DataSet("datasets/yml/given/users.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/given/users.yml"],
            ignoreCols = ["created_at", "updated_at"]
        )
        fun `失敗-Emailが既に利用されていた場合、その旨のエラーが返り、登録できない`() {
            // given:
            val user = object : UnregisteredUser {
                override val email: Email get() = Email.newWithoutValidation("paul-graham@example.com")
                override val password: Password get() = Password.newWithoutValidation("Passw0rd")
                override val username: Username get() = Username.newWithoutValidation("non-used-username")
            }

            // when:
            val actual = userRepository.register(user)

            // then:
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
            // given:
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

    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @SpringBootTest
    @DBRider
    @DisplayName("Emailから検索(パスワード付き)")
    class FindByEmailWithPasswordTest(@Autowired val userRepository: UserRepository) {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet("datasets/yml/given/users.yml")
        fun `成功-対象の登録済みユーザーが存在する場合、パスワード付きで登録済みユーザーが戻り値となる`() {
            // given:
            val searchingEmail = Email.newWithoutValidation("paul-graham@example.com")

            // when:
            val actual = userRepository.findByEmailWithPassword(searchingEmail)

            // then:
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
            // given:
            val searchingEmail = Email.newWithoutValidation("notfound@example.com")

            // when:
            val actual = userRepository.findByEmailWithPassword(searchingEmail)

            // then:
            val expected = UserRepository.FindByEmailWithPasswordError.NotFound(searchingEmail).left()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @SpringBootTest
    @DBRider
    @DisplayName("UserIdから検索")
    class FindByUserIdTest(@Autowired val userRepository: UserRepository) {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet("datasets/yml/given/users.yml")
        fun `成功-対象の登録済みユーザーが存在する場合、登録済みユーザーが戻り値となる`() {
            // given:
            val searchingUserId = UserId(1)

            // when:
            val actual = userRepository.findByUserId(searchingUserId)

            // then:
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
            // given:
            val searchingUserId = UserId(1)

            // when:
            val actual = userRepository.findByUserId(searchingUserId)

            // then:
            val expected = UserRepository.FindByUserIdError.NotFound(UserId(1)).left()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @SpringBootTest
    @DBRider
    @DisplayName("ユーザー情報更新")
    class UpdateTest(@Autowired val userRepository: UserRepository) {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet("datasets/yml/given/users.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/then/user_repository/update-success.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        // NOTE: @ExportDataSetはgivenの@DataSetが変更用に残しておく
        // @ExportDataSet(format = DataSetFormat.YML, outputName = "src/test/resources/datasets/yml/then/user_repository/update-success.yml", includeTables = ["users", "profiles", "followings"])
        fun `成功-更新可能な登録済みユーザーの場合、更新が反映された登録済みユーザーが戻り値となり、更新される`() {
            // given: Usernameだけ変更なしの更新可能な登録済みユーザー
            val updatableRegisteredUser = object : UpdatableRegisteredUser {
                override val userId: UserId get() = UserId(1)
                override val email: Email get() = Email.newWithoutValidation("paul-graham-2022@example.com")
                override val username: Username get() = Username.newWithoutValidation("paul-graham")
                override val bio: Bio get() = Bio.newWithoutValidation("Lisper, ハッカーと画家")
                override val image: Image get() = Image.newWithoutValidation("https://sep.yimg.com/ca/I/paulgraham_2239_13556")
            }

            // when:
            val actual = userRepository.update(updatableRegisteredUser)

            // then:
            val expected = RegisteredUser.newWithoutValidation(
                updatableRegisteredUser.userId,
                updatableRegisteredUser.email,
                updatableRegisteredUser.username,
                updatableRegisteredUser.bio,
                updatableRegisteredUser.image,
            ).right()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet("datasets/yml/given/users.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/given/users.yml"],
            ignoreCols = ["created_at", "updated_at"]
        )
        fun `失敗-Email が既に利用されていた場合、その旨のエラーが戻り値となり、更新されない`() {
            // given:
            val alreadyUsedEmailUpdatableRegisteredUser = object : UpdatableRegisteredUser {
                override val userId: UserId get() = UserId(1)
                override val email: Email get() = Email.newWithoutValidation("matz@example.com")
                override val username: Username get() = Username.newWithoutValidation("paul-graham")
                override val bio: Bio get() = Bio.newWithoutValidation("Lisper, ハッカーと画家")
                override val image: Image get() = Image.newWithoutValidation("https://sep.yimg.com/ca/I/paulgraham_2239_13556")
            }

            // when:
            val actual = userRepository.update(alreadyUsedEmailUpdatableRegisteredUser)

            // then:
            val expected = UserRepository.UpdateError.AlreadyRegisteredEmail(alreadyUsedEmailUpdatableRegisteredUser.email).left()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet("datasets/yml/given/users.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/given/users.yml"],
            ignoreCols = ["created_at", "updated_at"]
        )
        fun `失敗-Username が既に利用されていた場合、その旨のエラーが戻り値となり、更新されない`() {
            // given:
            val alreadyUsedUsernameUpdatableRegisteredUser = object : UpdatableRegisteredUser {
                override val userId: UserId get() = UserId(1)
                override val email: Email get() = Email.newWithoutValidation("paul-graham@example.com")
                override val username: Username get() = Username.newWithoutValidation("graydon-hoare")
                override val bio: Bio get() = Bio.newWithoutValidation("Lisper, ハッカーと画家")
                override val image: Image get() = Image.newWithoutValidation("https://sep.yimg.com/ca/I/paulgraham_2239_13556")
            }

            // when:
            val actual = userRepository.update(alreadyUsedUsernameUpdatableRegisteredUser)

            // then:
            val expected = UserRepository.UpdateError.AlreadyRegisteredUsername(alreadyUsedUsernameUpdatableRegisteredUser.username).left()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet("datasets/yml/given/empty-users.yml")
        @ExpectedDataSet(value = ["datasets/yml/given/empty-users.yml"])
        fun `失敗-対象の登録済みユーザーが存在しない場合、その旨のエラーが戻り値となり、更新されない`() {
            // given: Usernameだけ変更なしの更新可能な登録済みユーザー
            val notFoundUpdatableRegisteredUser = object : UpdatableRegisteredUser {
                override val userId: UserId get() = UserId(1)
                override val email: Email get() = Email.newWithoutValidation("paul-graham-2022@example.com")
                override val username: Username get() = Username.newWithoutValidation("paul-graham")
                override val bio: Bio get() = Bio.newWithoutValidation("Lisper, ハッカーと画家")
                override val image: Image get() = Image.newWithoutValidation("https://sep.yimg.com/ca/I/paulgraham_2239_13556")
            }

            // when:
            val actual = userRepository.update(notFoundUpdatableRegisteredUser)

            // then:
            val expected = UserRepository.UpdateError.NotFound(notFoundUpdatableRegisteredUser.userId).left()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class FindByUsername {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
            ]
        )
        fun `正常系-Usernameに該当する登録済みユーザーが存在する場合、登録済みユーザーが戻り値`() {
            /**
             * given:
             */
            val userRepository = UserRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val existedUsername = Username.newWithoutValidation("paul-graham")

            /**
             * when:
             */
            val actual = userRepository.findByUsername(existedUsername)

            /**
             * then:
             */
            when (actual) {
                is Left -> assert(false) { "原因: ${actual.value}" }
                is Right -> {
                    val user = actual.value
                    assertThat(user.username).isEqualTo(existedUsername)
                }
            }
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/empty-users.yml",
            ]
        )
        fun `準正常系-Usernameに該当する登録済みユーザーが存在しない場合、その旨を表現するエラーが戻り値`() {
            /**
             * given:
             */
            val userRepository = UserRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val notExistedUsername = Username.newWithoutValidation("paul-graham")

            /**
             * when:
             */
            val actual = userRepository.findByUsername(notExistedUsername)

            /**
             * then:
             */
            val expected = UserRepository.FindByUsernameError.NotFound(notExistedUsername).left()
            assertThat(actual).isEqualTo(expected)
        }
    }
}
