package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface ProfileRepository {
    fun show(username: Username): Either<ShowError, OtherUser> = TODO()
    sealed interface ShowError : MyError {
        data class NotFoundProfileByUsername(val username: Username) : ShowError, MyError.Basic
        data class Unexpected(override val cause: Throwable, val username: Username) : ShowError, MyError.MyErrorWithThrowable
    }

    fun follow(username: Username, currentUserId: UserId): Either<FollowError, Unit> = TODO()
    sealed interface FollowError : MyError {
        data class Unexpected(override val cause: Throwable, val username: Username, val currentUserId: UserId) : FollowError, MyError.MyErrorWithThrowable
    }

    fun unfollow(username: Username): Either<UnfollowError, OtherUser> = TODO()
    sealed interface UnfollowError : MyError {
        data class NotFoundProfileByUsername(val username: Username) : UnfollowError, MyError.Basic
        data class Unexpected(override val cause: Throwable) : UnfollowError, MyError.MyErrorWithThrowable
    }
}
