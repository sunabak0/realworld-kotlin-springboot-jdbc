package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.*
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.typeclasses.Semigroup
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Password
import com.example.realworldkotlinspringbootjdbc.infra.UserRepository
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface LoginUseCase {
    fun execute(email: String?, password: String?): Either<Error, RegisteredUser> = Error.NotImplemented.left()
    sealed interface Error : MyError {
        data class ValidationErrors(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class Unauthorized(val email: Email) : Error, MyError.Basic
        data class UnexpectedError(val email: Option<String>) : Error, MyError.Basic
        object NotImplemented : Error
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
            is Invalid -> LoginUseCase.Error.ValidationErrors(validatedInput.value).left()
            //
            // Email, Password両方ともバリデーションはOK -> User 検索 -> Password 比較
            //
            is Valid -> userRepository.findByEmailWithPassword(validatedInput.value.first).flatMap {
                    if (validatedInput.value.second == it.second) { it.first.right() }
                    else { LoginUseCase.Error.Unauthorized(validatedInput.value.first).left() }
                }
        }
    }
}
