package com.example.realworldkotlinspringbootjdbc.usecase.profile

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface ShowProfileUseCase {
    fun execute(username: String?, currentUser: Option<RegisteredUser>): Either<Error, OtherUser> =
        throw NotImplementedError()

    sealed interface Error : MyError {
        data class InvalidUsername(override val errors: List<MyError.ValidationError>) :
            Error,
            MyError.ValidationErrors

        data class NotFound(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class ShowProfileUseCaseImpl(
    val profileRepository: ProfileRepository
) : ShowProfileUseCase {
    override fun execute(
        username: String?,
        currentUser: Option<RegisteredUser>
    ): Either<ShowProfileUseCase.Error, OtherUser> {
        /**
         * Username のバリデーション
         * Invalid -> 早期リターン
         */
        val validatedUsername = Username.new(username).fold(
            { return ShowProfileUseCase.Error.InvalidUsername(it).left() },
            { it }
        )
        return when (currentUser) {
            /**
             * JWT 認証 失敗 or 未ログイン
             */
            is None -> when (val showProfileResult = profileRepository.show(validatedUsername)) {
                /**
                 * プロフィール取得失敗
                 */
                is Left -> when (val error = showProfileResult.value) {
                    is ProfileRepository.ShowError.NotFoundProfileByUsername -> ShowProfileUseCase.Error.NotFound(
                        error
                    ).left()
                }
                /**
                 * プロフィール取得成功
                 */
                is Right -> OtherUser.newWithoutValidation(
                    showProfileResult.value.userId,
                    showProfileResult.value.username,
                    showProfileResult.value.bio,
                    showProfileResult.value.image,
                    showProfileResult.value.following,
                ).right()
            }
            /**
             * JWT 認証成功
             */
            is Some -> when (
                val showProfileResult =
                    profileRepository.show(validatedUsername, currentUser.value.userId.toOption())
            ) {
                /**
                 * プロフィール取得失敗
                 */
                is Left -> when (val error = showProfileResult.value) {
                    is ProfileRepository.ShowError.NotFoundProfileByUsername -> ShowProfileUseCase.Error.NotFound(
                        error
                    ).left()
                }
                /**
                 * プロフィール取得成功
                 */
                is Right -> OtherUser.newWithoutValidation(
                    showProfileResult.value.userId,
                    showProfileResult.value.username,
                    showProfileResult.value.bio,
                    showProfileResult.value.image,
                    showProfileResult.value.following,
                ).right()
            }
        }
    }
}
