package com.example.realworldkotlinspringbootjdbc.usecase.user_and_authentication

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UpdatableRegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface UpdateUserUseCase {
    fun execute(
        currentUser: RegisteredUser,
        email: String?,
        username: String?,
        bio: String?,
        image: String?,
    ): Either<Error, RegisteredUser> = throw UnsupportedOperationException()

    sealed interface Error : MyError {
        data class InvalidAttributes(
            override val errors: NonEmptyList<MyError.ValidationError>,
            val currentUser: RegisteredUser,
        ) : Error, MyError.ValidationErrors
        data class AlreadyUsedEmail(
            override val cause: MyError,
            val updatableRegisteredUser: UpdatableRegisteredUser
        ) : Error, MyError.MyErrorWithMyError
        data class AlreadyUsedUsername(
            override val cause: MyError,
            val updatableRegisteredUser: UpdatableRegisteredUser
        ) : Error, MyError.MyErrorWithMyError
        data class NotFoundUser(
            override val cause: MyError,
            val currentUser: RegisteredUser
        ) : Error, MyError.MyErrorWithMyError
    }
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
    ): Either<UpdateUserUseCase.Error, RegisteredUser> {
        /**
         * バリデーション -> 更新可能な登録済みユーザー
         * 失敗 -> 早期return
         */
        val updatableRegisteredUser = UpdatableRegisteredUser.new(currentUser, email, username, bio, image).fold(
            { return UpdateUserUseCase.Error.InvalidAttributes(errors = it, currentUser = currentUser).left() },
            { it }
        )

        return when (val updateResult = userRepository.update(updatableRegisteredUser)) {
            /**
             * 更新: 失敗
             */
            is Left -> when (val error = updateResult.value) {
                /**
                 * 原因: Emailは誰かが使っていた
                 */
                is UserRepository.UpdateError.AlreadyRegisteredEmail -> UpdateUserUseCase.Error.AlreadyUsedEmail(
                    cause = error,
                    updatableRegisteredUser = updatableRegisteredUser,
                ).left()
                /**
                 * 原因: Usernameは誰かが使っていた
                 */
                is UserRepository.UpdateError.AlreadyRegisteredUsername -> UpdateUserUseCase.Error.AlreadyUsedUsername(
                    cause = error,
                    updatableRegisteredUser = updatableRegisteredUser
                ).left()
                /**
                 * 原因: ユーザーが見つからなかった(ほぼ考えられない)
                 */
                is UserRepository.UpdateError.NotFound -> UpdateUserUseCase.Error.NotFoundUser(
                    cause = error,
                    currentUser = currentUser
                ).left()
            }
            /**
             * 更新: 成功
             */
            is Right -> RegisteredUser.newWithoutValidation(
                userId = updatableRegisteredUser.userId,
                email = updatableRegisteredUser.email,
                username = updatableRegisteredUser.username,
                bio = updatableRegisteredUser.bio,
                image = updatableRegisteredUser.image,
            ).right()
        }
    }
}
