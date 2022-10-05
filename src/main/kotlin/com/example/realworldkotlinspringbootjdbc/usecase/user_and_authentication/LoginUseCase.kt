package com.example.realworldkotlinspringbootjdbc.usecase.user_and_authentication

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
import arrow.core.right
import arrow.core.zip
import arrow.typeclasses.Semigroup
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Password
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface LoginUseCase {
    fun execute(email: String?, password: String?): Either<Error, RegisteredUser> = throw NotImplementedError()
    sealed interface Error : MyError {
        data class InvalidEmailOrPassword(override val errors: List<MyError.ValidationError>) :
            Error,
            MyError.ValidationErrors

        data class Unauthorized(val email: Email) : Error, MyError.Basic
        data class Unexpected(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class LoginUseCaseImpl(
    val userRepository: UserRepository
) : LoginUseCase {
    override fun execute(email: String?, password: String?): Either<LoginUseCase.Error, RegisteredUser> {
        val validatedInput = Email.new(email).zip(
            Semigroup.nonEmptyList(),
            Password.newForLogin(password)
        ) { a, b -> Pair(a, b) }
        return when (validatedInput) {
            is Invalid -> LoginUseCase.Error.InvalidEmailOrPassword(validatedInput.value).left()
            /**
             * Email, Password両方ともバリデーションはOK -> User 検索
             */
            is Valid -> when (
                val registeredUserWithPassword =
                    userRepository.findByEmailWithPassword(validatedInput.value.first)
            ) {
                /**
                 * 何かしらのエラー
                 */
                is Left -> when (val error = registeredUserWithPassword.value) {
                    is UserRepository.FindByEmailWithPasswordError.NotFound -> LoginUseCase.Error.Unauthorized(error.email)
                        .left()

                    is UserRepository.FindByEmailWithPasswordError.Unexpected -> LoginUseCase.Error.Unexpected(error)
                        .left()
                }
                /**
                 * Found user by email
                 */
                is Right -> {
                    val aPassword = validatedInput.value.second
                    val (registeredUser, bPassword) = registeredUserWithPassword.value
                    /**
                     * 認証 成功/失敗
                     */
                    if (aPassword == bPassword) {
                        registeredUser.right()
                    } else {
                        LoginUseCase.Error.Unauthorized(
                            validatedInput.value.first
                        ).left()
                    }
                }
            }
        }
    }
}
