package com.example.realworldkotlinspringbootjdbc.repository

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UnregisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.service.UserService
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Repository

interface UserRepository {
    fun register(user: UnregisteredUser): Either<UserService.RegisterError, RegisteredUser>

    sealed interface UserRepositoryError : MyError {
        sealed interface TransactionError : UserRepositoryError {
            data class DbError(override val cause: Throwable, val unregisteredUser: UnregisteredUser) : UserRepositoryError, MyError.MyErrorWithThrowable
            data class UnexpectedError(override val cause: Throwable, val unregisteredUser: UnregisteredUser) : UserRepositoryError, MyError.MyErrorWithThrowable
        }
        object NotImplemented : UserRepositoryError
    }
}

@Repository
class UserRepositoryImpl : UserRepository {
    override fun register(user: UnregisteredUser): Either<UserService.RegisterError, RegisteredUser> {
        val userId = try {
            registerTransactionApply(user)
        } catch (e: Throwable) {
            val error = UserRepository.UserRepositoryError.TransactionError.DbError(e, user)
            return Either.Left(UserService.RegisterError.FailedRegister(error))
        }
        val registeredUser = object : RegisteredUser {
            override val userId: UserId get() = userId
            override val email: Email get() = user.email
            override val username: Username get() = user.username
            override val bio: Bio get() = object : Bio { override val value: String get() = "" }
            override val image: Image get() = object : Image { override val value: String get() = "" }
        }
        return Either.Right(registeredUser)
    }

    //
    // @Transaction
    fun registerTransactionApply(user: UnregisteredUser): UserId {
        // val sql0 = "SELECT count(email) FROM users WHERE users.email = ?"
        // val sql1 = "INSERT INTO users(email, username, password, created_at, updated_at) VALUES (:email, :username, :password, :created_at, :updated_at) RETURNING id;"
        // val sql2 = "INSERT INTO profiles(user_id, bio, image, created_at, updated_at) VALUES (:user_id, :bio, :image, :created_at, :updated_at);"
        return UserId(999)
    }
}
