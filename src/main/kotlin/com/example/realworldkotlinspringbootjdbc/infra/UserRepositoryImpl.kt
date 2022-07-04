package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UnregisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UpdatableRegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository.FindByEmailWithPasswordError
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.Password
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.Date

typealias RegisteredWithPassword = Pair<RegisteredUser, Password>

@Repository
class UserRepositoryImpl(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) : UserRepository {
    sealed interface Error : MyError {
        data class Unexpected(override val cause: Throwable) : Error, MyError.MyErrorWithThrowable
        data class NotFoundByEmail(val email: Email) : Error, MyError.Basic
    }

    override fun register(user: UnregisteredUser): Either<UserRepository.RegisterError, RegisteredUser> {
        /**
         * Email ユニーク check
         */
        val sql1 = """
            SELECT count(*)
            FROM users
            WHERE users.email = :email
            ;
        """.trimIndent()
        val sqlParams1 = MapSqlParameterSource().addValue("email", user.email.value)
        try {
            val count = namedParameterJdbcTemplate.queryForMap(sql1, sqlParams1)["count"] as Long
            if (0 < count) {
                return UserRepository.RegisterError.AlreadyRegisteredEmail(user.email).left()
            }
        } catch (e: Throwable) {
            return UserRepository.RegisterError.Unexpected(e, user).left()
        }

        /**
         * Username ユニーク check
         */
        val sql2 = """
            SELECT count(*)
            FROM users
            WHERE users.username = :username
            ;
        """.trimIndent()
        val sqlParams2 = MapSqlParameterSource().addValue("username", user.username.value)
        try {
            val count = namedParameterJdbcTemplate.queryForMap(sql2, sqlParams2)["count"] as Long
            if (0 < count) {
                return UserRepository.RegisterError.AlreadyRegisteredUsername(user.username).left()
            }
        } catch (e: Throwable) {
            return UserRepository.RegisterError.Unexpected(e, user).left()
        }

        /**
         * やっと登録処理
         */
        val userId = try {
            registerTransactionApply(user)
        } catch (e: Throwable) {
            // Timeoutや他の原因を別で扱いたかったら、Infra層でエラーを定義する
            return UserRepository.RegisterError.Unexpected(e, user).left()
        }
        val registeredUser = RegisteredUser.newWithoutValidation(
            userId,
            user.email,
            user.username,
            Bio.newWithoutValidation(""),
            Image.newWithoutValidation("")
        )
        return registeredUser.right()
    }

    /**
     * ユーザー登録処理
     */
    @Transactional
    fun registerTransactionApply(user: UnregisteredUser): UserId {
        val sql1 = "INSERT INTO users(email, username, password, created_at, updated_at) VALUES (:email, :username, :password, :created_at, :updated_at) RETURNING id;"
        val sql2 = "INSERT INTO profiles(user_id, bio, image, created_at, updated_at) VALUES (:user_id, :bio, :image, :created_at, :updated_at);"
        val sqlParams = MapSqlParameterSource()
            .addValue("email", user.email.value)
            .addValue("password", user.password.value)
            .addValue("username", user.username.value)
            .addValue("created_at", Date())
            .addValue("updated_at", Date())
            .addValue("bio", "")
            .addValue("image", "")
        val userId = namedParameterJdbcTemplate.queryForMap(sql1, sqlParams)["id"] as Int
        sqlParams.addValue("user_id", userId)
        namedParameterJdbcTemplate.update(sql2, sqlParams)
        return UserId(userId)
    }

    override fun findByEmailWithPassword(email: Email): Either<FindByEmailWithPasswordError, RegisteredWithPassword> {
        val sql = """
            SELECT
                users.id
                , users.email
                , users.username
                , users.password
                , profiles.bio
                , profiles.image
            FROM
                users
            JOIN
                profiles
            ON
                profiles.user_id = users.id
                AND users.email = :email
            ;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource().addValue("email", email.value)
        val users = try {
            namedParameterJdbcTemplate.queryForList(sql, sqlParams)
        } catch (e: Throwable) {
            return FindByEmailWithPasswordError.Unexpected(e, email).left()
        }

        if (users.isEmpty()) {
            return FindByEmailWithPasswordError.NotFound(email).left()
        }
        val userRecord = users.first()

        return try {
            val user = RegisteredUser.newWithoutValidation(
                UserId(userRecord["id"] as Int),
                Email.newWithoutValidation(userRecord["email"].toString()),
                Username.newWithoutValidation(userRecord["username"].toString()),
                Bio.newWithoutValidation(userRecord["bio"].toString()),
                Image.newWithoutValidation(userRecord["image"].toString()),
            )
            val password = Password.newWithoutValidation(userRecord["password"].toString())
            Pair(user, password).right()
        } catch (e: Throwable) {
            FindByEmailWithPasswordError.Unexpected(e, email).left()
        }
    }

    override fun findByUserId(id: UserId): Either<UserRepository.FindByUserIdError, RegisteredUser> {
        val registeredUser = RegisteredUser.newWithoutValidation(
            id,
            Email.newWithoutValidation("dummy@example.com"),
            Username.newWithoutValidation("dummy-username"),
            Bio.newWithoutValidation(""),
            Image.newWithoutValidation("")
        )
        return registeredUser.right()
    }

    override fun update(user: UpdatableRegisteredUser): Either<UserRepository.UpdateError, RegisteredUser> {
        val registeredUser = RegisteredUser.newWithoutValidation(
            user.userId,
            user.email,
            user.username,
            user.bio,
            user.image
        )
        return registeredUser.right()
    }

    private fun findByEmail(email: Email): Either<Error, RegisteredUser> {
        val sql = """
            SELECT *
            FROM users
                JOIN profiles ON profiles.user_id = users.id
            WHERE users.email = :email;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource()
            .addValue("email", email.value)
        val userList = try {
            namedParameterJdbcTemplate.queryForList(sql, sqlParams)
        } catch (e: Throwable) {
            return Error.Unexpected(e).left()
        }
        if (userList.isEmpty()) {
            return Error.NotFoundByEmail(email).left()
        }
        val userMap = userList[0]
        try {
            val userId = UserId(userMap["id"] as Int)
            val email = Email.newWithoutValidation(userMap["email"] as String)
        } catch (e: Throwable) {
            return Error.Unexpected(e).left()
        }
        TODO()
    }
}
