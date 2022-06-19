package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UnregisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Password
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.usecase.LoginUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.RegisterUserUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Repository
typealias RegisteredWithPassword = Pair<RegisteredUser, Password>

interface UserRepository {
    //
    // ユーザー登録
    //
    fun register(user: UnregisteredUser): Either<RegisterUserUseCase.Error, RegisteredUser> = RegisterUserUseCase.Error.NotImplemented.left()

    //
    // ユーザー検索 by Email with Password
    //
    fun findByEmailWithPassword(email: Email): Either<LoginUseCase.Error, RegisteredWithPassword> = LoginUseCase.Error.NotImplemented.left()

    sealed interface UserRepositoryError : MyError {
        sealed interface TransactionError : UserRepositoryError {
            data class DbError(override val cause: Throwable, val unregisteredUser: UnregisteredUser) : UserRepositoryError, MyError.MyErrorWithThrowable
            data class NotFoundError(val email: Email) : UserRepositoryError, MyError.Basic
            data class UnexpectedError(override val cause: Throwable, val unregisteredUser: UnregisteredUser) : UserRepositoryError, MyError.MyErrorWithThrowable
        }
    }
}

@Repository
class UserRepositoryImpl : UserRepository {
    override fun register(user: UnregisteredUser): Either<RegisterUserUseCase.Error, RegisteredUser> {
        val userId = try {
            registerTransactionApply(user)
        } catch (e: Throwable) {
            val error = UserRepository.UserRepositoryError.TransactionError.DbError(e, user)
            return RegisterUserUseCase.Error.FailedRegister(error).left()
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
    //
    // @Transaction
    fun registerTransactionApply(user: UnregisteredUser): UserId {
        // val sql0 = "SELECT count(email) FROM users WHERE users.email = ?"
        // val sql1 = "INSERT INTO users(email, username, password, created_at, updated_at) VALUES (:email, :username, :password, :created_at, :updated_at) RETURNING id;"
        // val sql2 = "INSERT INTO profiles(user_id, bio, image, created_at, updated_at) VALUES (:user_id, :bio, :image, :created_at, :updated_at);"
        return UserId(999)
    }

    override fun findByEmailWithPassword(email: Email): Either<LoginUseCase.Error, RegisteredWithPassword> {
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
