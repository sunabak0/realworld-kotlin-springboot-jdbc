package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Validated
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Profile
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface UnfollowProfileUseCase {
    fun execute(username: String?, currentUserId: UserId): Either<Error, Profile> = TODO()
    sealed interface Error : MyError {
        data class InvalidUsername(override val errors: List<MyError.ValidationError>) :
            Error,
            MyError.ValidationErrors

        data class NotFound(override val cause: MyError) : Error, MyError.MyErrorWithMyError
        data class Unexpected(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class UnfollowProfileUseCaseImpl(
    val profileRepository: ProfileRepository
) : UnfollowProfileUseCase {
    override fun execute(username: String?, currentUserId: UserId): Either<UnfollowProfileUseCase.Error, Profile> {
        return when (val it = Username.new(username)) {
            /**
             * Username が不正
             */
            is Validated.Invalid -> UnfollowProfileUseCase.Error.InvalidUsername(it.value).left()
            /**
             * Username が適切
             */
            is Validated.Valid -> when (val showProfileResult = profileRepository.show(it.value)) {
                /**
                 * プロフィール検索失敗
                 */
                is Left -> when (val error = showProfileResult.value) {
                    /**
                     * 原因: プロフィールが見つからなかった
                     */
                    is ProfileRepository.ShowError.NotFoundProfileByUsername -> TODO()
                    /**
                     * 原因: 不明
                     */
                    is ProfileRepository.ShowError.Unexpected -> TODO()
                }
                /**
                 * プロフィール取得成功
                 */
                is Right -> when (val unfollowProfileResult = profileRepository.unfollow(it.value, currentUserId)) {
                    /**
                     * アンフォロー失敗
                     */
                    is Left -> when (val error = unfollowProfileResult.value) {
                        /**
                         * 原因: 不明
                         */
                        is ProfileRepository.UnfollowError.Unexpected -> TODO()
                    }
                    /**
                     * アンフォロー成功
                     */
                    is Right -> Profile.newWithoutValidation(
                        showProfileResult.value.username,
                        showProfileResult.value.bio,
                        showProfileResult.value.image,
                        false
                    ).right()
                }
            }
        }
    }
}
