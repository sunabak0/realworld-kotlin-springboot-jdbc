package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Either
import arrow.core.Option
import arrow.core.none
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError

/**
 * 登録済みユーザーのRepository
 *
 */
interface ProfileRepository {
    fun show(username: Username): Either<ShowWithoutAuthorizedError, OtherUser> = throw NotImplementedError()
    sealed interface ShowWithoutAuthorizedError : MyError {
        data class NotFoundProfileByUsername(val username: Username) : ShowWithoutAuthorizedError, MyError.Basic
        data class Unexpected(override val cause: Throwable, val username: Username) :
            ShowWithoutAuthorizedError,
            MyError.MyErrorWithThrowable
    }

    fun show(username: Username, currentUserId: UserId): Either<ShowError, OtherUser> = throw NotImplementedError()
    sealed interface ShowError : MyError {
        data class NotFoundProfileByUsername(val username: Username, val currentUserId: UserId) :
            ShowError,
            MyError.Basic

        data class Unexpected(override val cause: Throwable, val username: Username, val currentUserId: UserId) :
            ShowError, MyError.MyErrorWithThrowable
    }

    fun follow(username: Username, currentUserId: UserId): Either<FollowError, OtherUser> = throw NotImplementedError()
    sealed interface FollowError : MyError {
        data class NotFoundProfileByUsername(val username: Username, val currentUserId: UserId) :
            FollowError,
            MyError.Basic

        data class Unexpected(override val cause: Throwable, val username: Username, val currentUserId: UserId) :
            FollowError, MyError.MyErrorWithThrowable
    }

    fun unfollow(username: Username, currentUserId: UserId): Either<UnfollowError, OtherUser> =
        throw NotImplementedError()

    sealed interface UnfollowError : MyError {
        data class NotFoundProfileByUsername(val username: Username, val currentUserId: UserId) :
            UnfollowError,
            MyError.Basic

        data class Unexpected(override val cause: Throwable, val username: Username, val currentUserId: UserId) :
            UnfollowError, MyError.MyErrorWithThrowable
    }

    /**
     * 登録済みユーザー検索 by ユーザー名
     *
     * - 特定のユーザー視点が有る場合
     *   - followingの有無がある
     *
     * @param username ユーザー名
     * @param viewpointUserId あるユーザー視点となるユーザーID
     * @return エラー or 登録済みユーザー
     */
    fun findByUsername(
        username: Username,
        viewpointUserId: Option<UserId> = none()
    ): Either<FindByUsernameError, OtherUser> = throw NotImplementedError()

    sealed interface FindByUsernameError : MyError {
        data class NotFound(val username: Username) : FindByUsernameError, MyError.Basic
    }

    /**
     * 登録済みユーザーフィルタ by 複数の登録済みユーザーID
     *
     * Optional: あるユーザー視点が見た場合
     * - ない場合
     *   - 見つかった登録済みユーザーは全て 未フォロー
     * - ある場合
     *   - 見つかった登録済みユーザーに対して、フォロー済 or 未フォロー の情報がある
     *
     * @param userIds 登録済みユーザーID郡
     * @param viewpointUserId あるユーザー視点となるユーザーID
     * @return エラー or 登録済みユーザー郡
     */
    fun filterByUserIds(
        userIds: Set<UserId>,
        viewpointUserId: Option<UserId> = none()
    ): Either<FilterByUserIdsError, Set<OtherUser>> = throw NotImplementedError()

    sealed interface FilterByUserIdsError : MyError

    /**
     * 対象の登録済みユーザーがフォローしているユーザー郡
     *
     * @param userId 対象の登録済みユーザーID
     * @return エラー or ユーザー郡
     */
    fun filterFollowedByUser(userId: UserId): Either<FilterFollowedByUserError, Set<OtherUser>> =
        throw NotImplementedError()

    sealed interface FilterFollowedByUserError : MyError
}
