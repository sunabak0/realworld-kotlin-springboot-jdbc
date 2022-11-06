package com.example.realworldkotlinspringbootjdbc.usecase.profile

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
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
        return when (val it = Username.new(username)) {
            /**
             * Username が不正
             */
            is Invalid -> ShowProfileUseCase.Error.InvalidUsername(it.value).left()
            /**
             * Username が適切
             */
            is Valid -> when (currentUser) {
                /**
                 * JWT 認証 失敗 or 未ログイン
                 */
                is None -> when (val showProfileResult = profileRepository.show(it.value)) {
                    /**
                     * プロフィール取得失敗
                     */
                    is Left -> when (val error = showProfileResult.value) {
                        is ProfileRepository.ShowWithoutAuthorizedError.NotFoundProfileByUsername -> ShowProfileUseCase.Error.NotFound(
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
                is Some -> when (val showProfileResult = profileRepository.show(it.value, currentUser.value.userId)) {
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
}
