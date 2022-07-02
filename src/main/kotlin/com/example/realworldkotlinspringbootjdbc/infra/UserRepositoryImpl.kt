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

typealias RegisteredWithPassword = Pair<RegisteredUser, Password>

@Repository
class UserRepositoryImpl(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) : UserRepository {
    sealed interface Error : MyError {
        data class Unexpected(override val cause: Throwable) : Error, MyError.MyErrorWithThrowable
        data class NotFoundByEmail(val email: Email) : Error, MyError.Basic
    }

    override fun register(user: UnregisteredUser): Either<UserRepository.RegisterError, RegisteredUser> {
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

    // @Transaction
    private fun registerTransactionApply(user: UnregisteredUser): UserId {
        // val sql0 = "SELECT count(email) FROM users WHERE users.email = ?"
        // val sql1 = "INSERT INTO users(email, username, password, created_at, updated_at) VALUES (:email, :username, :password, :created_at, :updated_at) RETURNING id;"
        // val sql2 = "INSERT INTO profiles(user_id, bio, image, created_at, updated_at) VALUES (:user_id, :bio, :image, :created_at, :updated_at);"

        return UserId(999)
    }

    // Transactionは不要(rollbackしないため)
    override fun findByEmailWithPassword(email: Email): Either<FindByEmailWithPasswordError, RegisteredWithPassword> {
        // val sql0 = "SELECT count(email) FROM users WHERE users.email = :email"
        // val sql1 = "SELECT * FROM users WHERE users.email = :email;"
        // val sql2 = "INSERT INTO profiles(user_id, bio, image, created_at, updated_at) VALUES (:user_id, :bio, :image, :created_at, :updated_at);"
        val registeredUser = RegisteredUser.newWithoutValidation(
            UserId(888),
            email,
            Username.newWithoutValidation("dummy-username"),
            Bio.newWithoutValidation(""),
            Image.newWithoutValidation("")
        )
        val password = Password.newWithoutValidation("dummy-password")
        return Pair(registeredUser, password).right()
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
            SELECT * FROM users WHERE users.email = :email;
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
