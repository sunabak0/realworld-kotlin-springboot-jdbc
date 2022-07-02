package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface FollowProfileUseCase {
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
class FollowProfileUseCaseImpl(val profileRepository: ProfileRepository) :
    FollowProfileUseCase {
    override fun execute(username: String?, currentUserId: UserId): Either<FollowProfileUseCase.Error, Profile> {
        return when (val it = Username.new(username)) {
            /**
             * Username が不正
             */
            is Invalid -> FollowProfileUseCase.Error.InvalidUsername(it.value).left()
            /**
             * Username が適切
             */
            is Valid -> when (val showProfileResult = profileRepository.show(it.value)) {
                /**
                 * プロフィール検索失敗
                 */
                is Left -> when (val error = showProfileResult.value) {
                    /**
                     * 原因: プロフィールが見つからなかった
                     */
                    is ProfileRepository.ShowError.NotFoundProfileByUsername -> FollowProfileUseCase.Error.NotFound(
                        error
                    ).left()
                    /**
                     * 原因: 不明
                     */
                    is ProfileRepository.ShowError.Unexpected -> FollowProfileUseCase.Error.Unexpected(error).left()
                }
                /**
                 * プロフィールフォロー失敗
                 */
                is Left -> TODO()
                /**
                 * プロフィールフォロー成功
                 */
                is Right -> TODO()
            }
        }
    }
}
