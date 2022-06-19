package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UnregisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository.FindByEmailWithPasswordError
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Password
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.springframework.stereotype.Repository
typealias RegisteredWithPassword = Pair<RegisteredUser, Password>

@Repository
class UserRepositoryImpl : UserRepository {
    override fun register(user: UnregisteredUser): Either<UserRepository.RegisterError, RegisteredUser> {
        val userId = try {
            registerTransactionApply(user)
        } catch (e: Throwable) {
            // Timeoutや他の原因を別で扱いたかったら、Infra層でエラーを定義する
            return UserRepository.RegisterError.Unexpected(e, user).left()
        }
        val registeredUser = RegisteredUser.newWithoutValidation(
            userId.value,
            user.email.value,
            user.username.value,
            "",
            ""
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
            888,
            email.value,
            "dummy-username",
            "",
            ""
        )
        val password = Password.newWithoutValidation("dummy-password")
        return Pair(registeredUser, password).right()
    }
}
