package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Profile
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface UnfollowProfileUseCase {
    fun execute(username: String?, currentUser: RegisteredUser): Either<Error, Profile> = TODO()
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
    override fun execute(
        username: String?,
        currentUser: RegisteredUser
    ): Either<UnfollowProfileUseCase.Error, Profile> {
        return when (val it = Username.new(username)) {
            /**
             * Username が不正
             */
            is Invalid -> UnfollowProfileUseCase.Error.InvalidUsername(it.value).left()
            /**
             * Username が適切
             */
            is Valid -> when (val unfollowResult = profileRepository.unfollow(it.value, currentUser.userId)) {
                /**
                 * アンフォロー 失敗
                 */
                is Left -> when (val error = unfollowResult.value) {
                    /**
                     * 原因: プロフィールが見つからなかった
                     */
                    is ProfileRepository.UnfollowError.NotFoundProfileByUsername -> TODO()
                    /**
                     * 原因: 不明
                     */
                    is ProfileRepository.UnfollowError.Unexpected -> TODO()
                }
                /**
                 * アンフォロー 成功
                 */
                is Right -> Profile.newWithoutValidation(
                    unfollowResult.value.username,
                    unfollowResult.value.bio,
                    unfollowResult.value.image,
                    unfollowResult.value.following,
                ).right()
            }
        }
    }
}
