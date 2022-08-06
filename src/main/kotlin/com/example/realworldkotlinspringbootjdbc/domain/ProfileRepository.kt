package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface ProfileRepository {
    fun show(username: Username): Either<ShowWithoutAuthorizedError, OtherUser> = TODO()
    sealed interface ShowWithoutAuthorizedError : MyError {
        data class NotFoundProfileByUsername(val username: Username) : ShowWithoutAuthorizedError, MyError.Basic
        data class Unexpected(override val cause: Throwable, val username: Username) :
            ShowWithoutAuthorizedError,
            MyError.MyErrorWithThrowable
    }

    fun show(username: Username, currentUserId: UserId): Either<ShowError, OtherUser> = TODO()
    sealed interface ShowError : MyError {
        data class NotFoundProfileByUsername(val username: Username, val currentUserId: UserId) :
            ShowError,
            MyError.Basic

        data class Unexpected(override val cause: Throwable, val username: Username, val currentUserId: UserId) :
            ShowError, MyError.MyErrorWithThrowable
    }

    fun follow(username: Username, currentUserId: UserId): Either<FollowError, OtherUser> = TODO()
    sealed interface FollowError : MyError {
        data class NotFoundProfileByUsername(val username: Username, val currentUserId: UserId) :
            FollowError,
            MyError.Basic

        data class Unexpected(override val cause: Throwable, val username: Username, val currentUserId: UserId) :
            FollowError, MyError.MyErrorWithThrowable
    }

    fun unfollow(username: Username, currentUserId: UserId): Either<UnfollowError, OtherUser> = TODO()
    sealed interface UnfollowError : MyError {
        data class NotFoundProfileByUsername(val username: Username, val currentUserId: UserId) :
            UnfollowError,
            MyError.Basic

        data class Unexpected(override val cause: Throwable, val username: Username, val currentUserId: UserId) :
            UnfollowError, MyError.MyErrorWithThrowable
    }
}
