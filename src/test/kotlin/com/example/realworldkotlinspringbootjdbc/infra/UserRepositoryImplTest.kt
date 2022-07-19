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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import java.text.SimpleDateFormat

class UserRepositoryImplTest {
    companion object {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
        fun resetDb() {
            val sql = """
                DELETE FROM users;
                DELETE FROM profiles;
            """.trimIndent()
            namedParameterJdbcTemplate.update(sql, MapSqlParameterSource())
        }

        fun resetSequence() {
            val sql = """
                SELECT
                    setval('users_id_seq', 10000)
                    , setval('profiles_id_seq', 10000)
                ;
            """.trimIndent()
            namedParameterJdbcTemplate.queryForRowSet(sql, MapSqlParameterSource())
        }
    }

    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @SpringBootTest
    @DBRider
    @DisplayName("ユーザー登録")
    class RegisterTest(@Autowired val userRepository: UserRepository) {
        @BeforeAll
        fun reset() = resetSequence()

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

        @Nested
        @Tag("WithLocalDb")
        class `findByUserId(UserIdでユーザー検索)` {
            @BeforeEach
            fun reset() {
                resetDb()
            }

            @Test
            fun `該当するユーザーが存在する場合、ユーザーが戻り値となる`() {
                fun localPrepare() { // 事前に User を 1 レコード分追加
                    val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
                    val sql1 =
                        "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
                    val sqlParams1 = MapSqlParameterSource()
                        .addValue("id", 1)
                        .addValue("email", "dummy@example.com")
                        .addValue("username", "dummy-username")
                        .addValue("password", "Passw0rd")
                        .addValue("created_at", date)
                        .addValue("updated_at", date)
                    namedParameterJdbcTemplate.update(sql1, sqlParams1)
                    val sql2 =
                        "INSERT INTO profiles(id, user_id, bio, image, created_at, updated_at) VALUES (:id, :user_id, :bio, :image, :created_at, :updated_at);"
                    val sqlParams2 = MapSqlParameterSource()
                        .addValue("id", 1)
                        .addValue("user_id", 1)
                        .addValue("bio", "dummy-bio")
                        .addValue("image", "dummy-image")
                        .addValue("created_at", date)
                        .addValue("updated_at", date)
                    namedParameterJdbcTemplate.update(sql2, sqlParams2)
                }
                localPrepare()
                val repository = UserRepositoryImpl(namedParameterJdbcTemplate)

                val actual = repository.findByUserId(UserId(1))
                val expected = RegisteredUser.newWithoutValidation(
                    UserId(1),
                    Email.newWithoutValidation("dummy@example.com"),
                    Username.newWithoutValidation("dummy-username"),
                    Bio.newWithoutValidation("dummy-bio"),
                    Image.newWithoutValidation("dummy-image"),
                ).right()
                assertThat(actual).isEqualTo(expected)
            }

            @Test
            fun `該当するユーザーが存在しない場合、その旨のエラーが戻り値となる`() {
                val repository = UserRepositoryImpl(namedParameterJdbcTemplate)

                val actual = repository.findByUserId(UserId(1))
                val expected = UserRepository.FindByUserIdError.NotFound(UserId(1)).left()
                assertThat(actual).isEqualTo(expected)
            }
        }
    }

    @Nested
    @Tag("WithLocalDb")
    class `findByUserId(UserIdでユーザー検索)` {
        @BeforeEach
        fun reset() {
            resetDb()
        }

        @Test
        fun `該当するユーザーが存在する場合、ユーザーが戻り値となる`() {
            fun localPrepare() { // 事前に User を 1 レコード分追加
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
                val sql1 =
                    "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
                val sqlParams1 = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("email", "dummy@example.com")
                    .addValue("username", "dummy-username")
                    .addValue("password", "Passw0rd")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(sql1, sqlParams1)
                val sql2 =
                    "INSERT INTO profiles(id, user_id, bio, image, created_at, updated_at) VALUES (:id, :user_id, :bio, :image, :created_at, :updated_at);"
                val sqlParams2 = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("user_id", 1)
                    .addValue("bio", "dummy-bio")
                    .addValue("image", "dummy-image")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(sql2, sqlParams2)
            }
            localPrepare()
            val repository = UserRepositoryImpl(namedParameterJdbcTemplate)

            val actual = repository.findByUserId(UserId(1))
            val expected = RegisteredUser.newWithoutValidation(
                UserId(1),
                Email.newWithoutValidation("dummy@example.com"),
                Username.newWithoutValidation("dummy-username"),
                Bio.newWithoutValidation("dummy-bio"),
                Image.newWithoutValidation("dummy-image"),
            ).right()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `該当するユーザーが存在しない場合、その旨のエラーが戻り値となる`() {
            val repository = UserRepositoryImpl(namedParameterJdbcTemplate)

            val actual = repository.findByUserId(UserId(1))
            val expected = UserRepository.FindByUserIdError.NotFound(UserId(1)).left()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @Tag("WithLocalDb")
    class `update(ユーザーの更新)` {
        @BeforeEach
        @AfterEach
        fun reset() {
            resetDb()
        }

        @Test
        fun `該当するユーザーが存在し、更新したい項目が全ての場合、全て更新されたユーザーが戻り値となる`() {
            fun localPrepare() { // 事前に User を 1 レコード分追加
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
                val sql1 =
                    "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
                val sqlParams1 = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("email", "dummy@example.com")
                    .addValue("username", "dummy-username")
                    .addValue("password", "Passw0rd")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(sql1, sqlParams1)
                val sql2 =
                    "INSERT INTO profiles(id, user_id, bio, image, created_at, updated_at) VALUES (:id, :user_id, :bio, :image, :created_at, :updated_at);"
                val sqlParams2 = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("user_id", 1)
                    .addValue("bio", "dummy-bio")
                    .addValue("image", "dummy-image")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(sql2, sqlParams2)
            }
            localPrepare()
            val repository = UserRepositoryImpl(namedParameterJdbcTemplate)

            val newUser = object : UpdatableRegisteredUser {
                override val userId: UserId get() = UserId(1)
                override val email: Email get() = Email.newWithoutValidation("new@example.com")
                override val username: Username get() = Username.newWithoutValidation("new-dummy-username")
                override val bio: Bio get() = Bio.newWithoutValidation("new-dummy-bio")
                override val image: Image get() = Image.newWithoutValidation("new-dummy-image")
            }

            val actual = repository.update(newUser)
            val expected = RegisteredUser.newWithoutValidation(
                newUser.userId,
                newUser.email,
                newUser.username,
                newUser.bio,
                newUser.image,
            ).right()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `該当するユーザーが存在し、更新したい項目が Bio と Image だけの場合、それだけ更新されたユーザーが戻り値となる`() {
            fun localPrepare() { // 事前に User を 1 レコード分追加
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
                val sql1 =
                    "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
                val sqlParams1 = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("email", "dummy@example.com")
                    .addValue("username", "dummy-username")
                    .addValue("password", "Passw0rd")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(sql1, sqlParams1)
                val sql2 =
                    "INSERT INTO profiles(id, user_id, bio, image, created_at, updated_at) VALUES (:id, :user_id, :bio, :image, :created_at, :updated_at);"
                val sqlParams2 = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("user_id", 1)
                    .addValue("bio", "dummy-bio")
                    .addValue("image", "dummy-image")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(sql2, sqlParams2)
            }
            localPrepare()
            val repository = UserRepositoryImpl(namedParameterJdbcTemplate)

            val newUser = object : UpdatableRegisteredUser {
                override val userId: UserId get() = UserId(1)
                override val email: Email get() = Email.newWithoutValidation("dummy@example.com")
                override val username: Username get() = Username.newWithoutValidation("dummy-username")
                override val bio: Bio get() = Bio.newWithoutValidation("new-dummy-bio")
                override val image: Image get() = Image.newWithoutValidation("new-dummy-image")
            }

            val actual = repository.update(newUser)
            val expected = RegisteredUser.newWithoutValidation(
                newUser.userId,
                newUser.email,
                newUser.username,
                newUser.bio,
                newUser.image,
            ).right()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `更新しようとした Email が既に登録済みだった場合、その旨のエラーが戻り値となる`() {
            fun localPrepare() { // 事前に User を 2 人分追加
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
                val sql1 =
                    "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
                val sqlParams1 = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("email", "dummy@example.com")
                    .addValue("username", "dummy-username")
                    .addValue("password", "Passw0rd")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(sql1, sqlParams1)
                val sql2 =
                    "INSERT INTO profiles(id, user_id, bio, image, created_at, updated_at) VALUES (:id, :user_id, :bio, :image, :created_at, :updated_at);"
                val sqlParams2 = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("user_id", 1)
                    .addValue("bio", "dummy-bio")
                    .addValue("image", "dummy-image")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(sql2, sqlParams2)

                val sql3 =
                    "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
                val sqlParams3 = MapSqlParameterSource()
                    .addValue("id", 2)
                    .addValue("email", "dummy2@example.com")
                    .addValue("username", "dummy-username2")
                    .addValue("password", "Passw0rd")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(sql3, sqlParams3)
                val sql4 =
                    "INSERT INTO profiles(id, user_id, bio, image, created_at, updated_at) VALUES (:id, :user_id, :bio, :image, :created_at, :updated_at);"
                val sqlParams4 = MapSqlParameterSource()
                    .addValue("id", 2)
                    .addValue("user_id", 2)
                    .addValue("bio", "dummy-bio")
                    .addValue("image", "dummy-image")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(sql4, sqlParams4)
            }
            localPrepare()
            val repository = UserRepositoryImpl(namedParameterJdbcTemplate)

            val newUser = object : UpdatableRegisteredUser {
                override val userId: UserId get() = UserId(1)
                override val email: Email get() = Email.newWithoutValidation("dummy2@example.com")
                override val username: Username get() = Username.newWithoutValidation("dummy-username")
                override val bio: Bio get() = Bio.newWithoutValidation("new-dummy-bio")
                override val image: Image get() = Image.newWithoutValidation("new-dummy-image")
            }

            val actual = repository.update(newUser)
            val expected = UserRepository.UpdateError.AlreadyRegisteredEmail(newUser.email).left()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `更新しようとした Username が既に登録済みだった場合、その旨のエラーが戻り値となる`() {
            fun localPrepare() { // 事前に User を 2 人分追加
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
                val sql1 =
                    "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
                val sqlParams1 = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("email", "dummy@example.com")
                    .addValue("username", "dummy-username")
                    .addValue("password", "Passw0rd")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(sql1, sqlParams1)
                val sql2 =
                    "INSERT INTO profiles(id, user_id, bio, image, created_at, updated_at) VALUES (:id, :user_id, :bio, :image, :created_at, :updated_at);"
                val sqlParams2 = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("user_id", 1)
                    .addValue("bio", "dummy-bio")
                    .addValue("image", "dummy-image")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(sql2, sqlParams2)

                val sql3 =
                    "INSERT INTO users(id, email, username, password, created_at, updated_at) VALUES (:id, :email, :username, :password, :created_at, :updated_at);"
                val sqlParams3 = MapSqlParameterSource()
                    .addValue("id", 2)
                    .addValue("email", "dummy2@example.com")
                    .addValue("username", "dummy-username2")
                    .addValue("password", "Passw0rd")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(sql3, sqlParams3)
                val sql4 =
                    "INSERT INTO profiles(id, user_id, bio, image, created_at, updated_at) VALUES (:id, :user_id, :bio, :image, :created_at, :updated_at);"
                val sqlParams4 = MapSqlParameterSource()
                    .addValue("id", 2)
                    .addValue("user_id", 2)
                    .addValue("bio", "dummy-bio")
                    .addValue("image", "dummy-image")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(sql4, sqlParams4)
            }
            localPrepare()
            val repository = UserRepositoryImpl(namedParameterJdbcTemplate)

            val newUser = object : UpdatableRegisteredUser {
                override val userId: UserId get() = UserId(1)
                override val email: Email get() = Email.newWithoutValidation("dummy@example.com")
                override val username: Username get() = Username.newWithoutValidation("dummy-username2")
                override val bio: Bio get() = Bio.newWithoutValidation("new-dummy-bio")
                override val image: Image get() = Image.newWithoutValidation("new-dummy-image")
            }

            val actual = repository.update(newUser)
            val expected = UserRepository.UpdateError.AlreadyRegisteredUsername(newUser.username).left()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `該当するユーザーが存在しない場合、その旨のエラーが戻り値となる`() {
            val repository = UserRepositoryImpl(namedParameterJdbcTemplate)

            val newUser = object : UpdatableRegisteredUser {
                override val userId: UserId get() = UserId(1)
                override val email: Email get() = Email.newWithoutValidation("new@example.com")
                override val username: Username get() = Username.newWithoutValidation("new-dummy-username")
                override val bio: Bio get() = Bio.newWithoutValidation("new-dummy-bio")
                override val image: Image get() = Image.newWithoutValidation("new-dummy-image")
            }

            val actual = repository.update(newUser)
            val expected = UserRepository.UpdateError.NotFound(newUser.userId).left()
            assertThat(actual).isEqualTo(expected)
        }
    }
}
