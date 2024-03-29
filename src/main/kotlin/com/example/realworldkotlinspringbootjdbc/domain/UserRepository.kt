package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Password
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface UserRepository {
    /**
     * ユーザー登録
     */
    fun register(user: UnregisteredUser): Either<RegisterError, RegisteredUser> = throw NotImplementedError()
    sealed interface RegisterError : MyError {
        data class AlreadyRegisteredEmail(val email: Email) : RegisterError, MyError.Basic
        data class AlreadyRegisteredUsername(val username: Username) : RegisterError, MyError.Basic
    }

    /**
     * ユーザー検索 by Email with Password
     */
    fun findByEmailWithPassword(email: Email): Either<FindByEmailWithPasswordError, RegisteredWithPassword> =
        throw NotImplementedError()

    sealed interface FindByEmailWithPasswordError : MyError {
        data class NotFound(val email: Email) : FindByEmailWithPasswordError, MyError.Basic
    }

    /**
     * ユーザー検索 by UserId
     */
    fun findByUserId(id: UserId): Either<FindByUserIdError, RegisteredUser> = throw NotImplementedError()
    sealed interface FindByUserIdError : MyError {
        data class NotFound(val id: UserId) : FindByUserIdError, MyError.Basic
    }

    /**
     * ユーザー情報更新
     * (ユーザーが居る前提の挙動になる)
     */
    fun update(user: UpdatableRegisteredUser): Either<UpdateError, Unit> = throw NotImplementedError()
    sealed interface UpdateError : MyError {
        data class NotFound(val userId: UserId) : UpdateError, MyError.Basic
        data class AlreadyRegisteredEmail(val email: Email) : UpdateError, MyError.Basic
        data class AlreadyRegisteredUsername(val username: Username) : UpdateError, MyError.Basic
    }
}

typealias RegisteredWithPassword = Pair<RegisteredUser, Password>
