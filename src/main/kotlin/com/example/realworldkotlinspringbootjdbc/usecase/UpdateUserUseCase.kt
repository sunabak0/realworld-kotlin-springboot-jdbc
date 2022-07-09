package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UpdatableRegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface UpdateUserUseCase {
    fun execute(
        currentUser: RegisteredUser,
        email: String?,
        username: String?,
        bio: String?,
        image: String?,
    ): Either<Error, UpdatedUser> = TODO()

    sealed interface Error : MyError {
        data class InvalidAttributesForUpdateUser(val currentUser: RegisteredUser, val errors: NonEmptyList<MyError>) : Error, MyError.Basic
        data class NoChange(val currentUser: RegisteredUser) : Error, MyError.Basic
        data class NotFound(val currentUser: RegisteredUser) : Error, MyError.Basic
        data class Unexpected(val currentUser: RegisteredUser, override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }

    data class UpdatedUser(
        val email: Email,
        val username: Username,
        val bio: Bio,
        val image: Image,
    )
}

@Service
class UpdateUserUseCaseImpl(
    val userRepository: UserRepository
) : UpdateUserUseCase {
    override fun execute(
        currentUser: RegisteredUser,
        email: String?,
        username: String?,
        bio: String?,
        image: String?
    ): Either<UpdateUserUseCase.Error, UpdateUserUseCase.UpdatedUser> =
        when (val updatableRegisteredUser = UpdatableRegisteredUser.new(currentUser, email, username, bio, image)) {
            /**
             * 更新 不可
             */
            is Invalid -> UpdateUserUseCase.Error.InvalidAttributesForUpdateUser(
                currentUser,
                updatableRegisteredUser.value
            ).left()
            /**
             * 更新 可能
             */
            is Valid -> when (val updatedResult = userRepository.update(updatableRegisteredUser.value)) {
                /**
                 * 更新 失敗
                 */
                is Either.Left -> when (val error = updatedResult.value) {
                    /**
                     * 原因: Emailが既に登録されている
                     */
                    is UserRepository.UpdateError.AlreadyRegisteredEmail -> UpdateUserUseCase.Error.InvalidAttributesForUpdateUser(
                        currentUser,
                        nonEmptyListOf(error)
                    ).left()
                    /**
                     * 原因: Emailが既に登録されている
                     */
                    is UserRepository.UpdateError.AlreadyRegisteredUsername -> UpdateUserUseCase.Error.InvalidAttributesForUpdateUser(
                        currentUser, nonEmptyListOf(error)
                    ).left()
                    /**
                     * 原因: ユーザーが見つからない
                     */
                    is UserRepository.UpdateError.NotFound -> UpdateUserUseCase.Error.NotFound(
                        currentUser
                    ).left()
                    /**
                     * 原因: 予期せぬエラー
                     */
                    is UserRepository.UpdateError.Unexpected -> UpdateUserUseCase.Error.Unexpected(
                        currentUser,
                        error
                    ).left()
                }
                /**
                 * 更新 成功
                 */
                is Either.Right -> UpdateUserUseCase.UpdatedUser(
                    updatedResult.value.email,
                    updatedResult.value.username,
                    updatedResult.value.bio,
                    updatedResult.value.image,
                ).right()
            }
        }
}
