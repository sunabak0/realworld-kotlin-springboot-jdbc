package com.example.realworldkotlinspringbootjdbc.usecase.profile

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface FollowProfileUseCase {
    fun execute(username: String?, currentUser: RegisteredUser): Either<Error, OtherUser> = throw NotImplementedError()
    sealed interface Error : MyError {
        data class InvalidUsername(override val errors: List<MyError.ValidationError>) :
            Error,
            MyError.ValidationErrors

        data class NotFound(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class FollowProfileUseCaseImpl(val profileRepository: ProfileRepository) :
    FollowProfileUseCase {
    override fun execute(
        username: String?,
        currentUser: RegisteredUser
    ): Either<FollowProfileUseCase.Error, OtherUser> {
        return when (val it = Username.new(username)) {
            /**
             * Username が不正
             */
            is Invalid -> FollowProfileUseCase.Error.InvalidUsername(it.value).left()
            /**
             * Username が適切
             */
            is Valid -> when (val followResult = profileRepository.follow(it.value, currentUser.userId)) {
                /**
                 * フォロー 失敗
                 */
                is Left -> when (val error = followResult.value) {
                    /**
                     * 原因: プロフィールが見つからなかった
                     */
                    is ProfileRepository.FollowError.NotFoundProfileByUsername -> FollowProfileUseCase.Error.NotFound(
                        error
                    ).left()
                }
                /**
                 * フォロー 成功
                 */
                is Right -> OtherUser.newWithoutValidation(
                    followResult.value.userId,
                    followResult.value.username,
                    followResult.value.bio,
                    followResult.value.image,
                    followResult.value.following
                ).right()
            }
        }
    }
}
