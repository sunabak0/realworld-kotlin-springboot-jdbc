package com.example.realworldkotlinspringbootjdbc.usecase.user_and_authentication

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UnregisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

/**
 * ユーザー登録
 */
interface RegisterUserUseCase {
    fun execute(email: String?, password: String?, username: String?): Either<Error, RegisteredUser> =
        throw NotImplementedError()

    sealed interface Error : MyError {
        data class InvalidUser(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class AlreadyRegisteredEmail(override val cause: MyError, val user: UnregisteredUser) :
            Error,
            MyError.MyErrorWithMyError

        data class AlreadyRegisteredUsername(override val cause: MyError, val user: UnregisteredUser) :
            Error,
            MyError.MyErrorWithMyError
    }
}

@Service
class RegisterUserUseCaseImpl(
    val userRepository: UserRepository
) : RegisterUserUseCase {
    override fun execute(
        email: String?,
        password: String?,
        username: String?
    ): Either<RegisterUserUseCase.Error, RegisteredUser> =
        when (val user = UnregisteredUser.new(email, password, username)) {
            is Invalid -> RegisterUserUseCase.Error.InvalidUser(user.value).left()
            is Valid -> when (val registerResult = userRepository.register(user.value)) {
                /**
                 * ユーザー登録 成功
                 */
                is Right -> registerResult.value.right()
                /**
                 * ユーザー登録 失敗
                 */
                is Left -> when (val registerError = registerResult.value) {
                    /**
                     * 原因: Emailが既に登録されている
                     */
                    is UserRepository.RegisterError.AlreadyRegisteredEmail -> RegisterUserUseCase.Error.AlreadyRegisteredEmail(
                        registerError,
                        user.value
                    ).left()
                    /**
                     * 原因: Usernameが既に登録されている
                     */
                    is UserRepository.RegisterError.AlreadyRegisteredUsername -> RegisterUserUseCase.Error.AlreadyRegisteredUsername(
                        registerError,
                        user.value
                    ).left()
                }
            }
        }
}
