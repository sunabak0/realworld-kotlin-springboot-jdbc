package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Validated
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.Profile
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface UnfollowProfileUseCase {
    fun execute(username: String?): Either<Error, Profile> = TODO()
    sealed interface Error : MyError {
        data class InvalidUserName(override val errors: List<MyError.ValidationError>) :
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
    override fun execute(username: String?): Either<UnfollowProfileUseCase.Error, Profile> {
        return when (val it = Username.new(username)) {
            /**
             * Username が不正
             */
            is Validated.Invalid -> UnfollowProfileUseCase.Error.InvalidUserName(it.value).left()
            /**
             * Username が適切
             */
            is Validated.Valid -> when (val unfollowProfileResult = profileRepository.unfollow(it.value)) {
                /**
                 * プロフィール取得失敗
                 */
                is Left -> TODO()
                /**
                 * プロフィール取得成功
                 */
                is Right -> TODO()
            }
        }
    }
}
