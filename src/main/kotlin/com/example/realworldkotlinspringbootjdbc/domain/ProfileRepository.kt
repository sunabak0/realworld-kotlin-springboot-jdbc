package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface ProfileRepository {
    fun show(username: Username): Either<ShowError, Profile> = TODO()
    sealed interface ShowError : MyError {
        data class NotFoundProfileByUsername(val username: Username) : ShowError, MyError.Basic
        data class Unexpected(override val cause: Throwable) : ShowError, MyError.MyErrorWithThrowable
    }
}
