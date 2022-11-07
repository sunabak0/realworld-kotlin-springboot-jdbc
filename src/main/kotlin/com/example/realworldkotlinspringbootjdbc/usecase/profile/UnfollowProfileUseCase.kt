package com.example.realworldkotlinspringbootjdbc.usecase.profile

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface UnfollowProfileUseCase {
    fun execute(username: String?, currentUser: RegisteredUser): Either<Error, OtherUser> = throw NotImplementedError()
    sealed interface Error : MyError {
        data class InvalidUsername(override val errors: List<MyError.ValidationError>) :
            Error,
            MyError.ValidationErrors

        data class NotFound(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class UnfollowProfileUseCaseImpl(
    val profileRepository: ProfileRepository
) : UnfollowProfileUseCase {
    override fun execute(
        username: String?,
        currentUser: RegisteredUser
    ): Either<UnfollowProfileUseCase.Error, OtherUser> {
        /**
         * Username のバリデーション
         * Invalid -> 早期 return
         */
        val validatedUsername = Username.new(username).fold(
            { return UnfollowProfileUseCase.Error.InvalidUsername(it).left() },
            { it }
        )

        return when (val unfollowResult = profileRepository.unfollow(validatedUsername, currentUser.userId)) {
            /**
             * アンフォロー 失敗
             */
            is Left -> when (val error = unfollowResult.value) {
                /**
                 * 原因: プロフィールが見つからなかった
                 */
                is ProfileRepository.UnfollowError.NotFoundProfileByUsername -> UnfollowProfileUseCase.Error.NotFound(
                    error
                ).left()
            }

            /**
             * アンフォロー 成功
             */
            is Right -> OtherUser.newWithoutValidation(
                unfollowResult.value.userId,
                unfollowResult.value.username,
                unfollowResult.value.bio,
                unfollowResult.value.image,
                unfollowResult.value.following,
            ).right()
        }
    }
}
