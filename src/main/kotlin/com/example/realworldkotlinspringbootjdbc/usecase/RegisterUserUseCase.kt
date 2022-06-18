package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UnregisteredUser
import com.example.realworldkotlinspringbootjdbc.infra.UserRepository
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

//
// ユーザー登録
//
interface RegisterUserUseCase {
    fun execute(email: String?, password: String?, username: String?): Either<Error, RegisteredUser> = Error.NotImplemented.left()
    sealed interface Error : MyError {
        data class ValidationErrors(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class FailedRegister(override val cause: MyError) : Error, MyError.MyErrorWithMyError
        object NotImplemented : Error
    }
}

@Service
class RegisterUserUseCaseImpl(
    val userRepository: UserRepository
) : RegisterUserUseCase {
    override fun execute(email: String?, password: String?, username: String?): Either<RegisterUserUseCase.Error, RegisteredUser> =
        when (val it = UnregisteredUser.new(email, password, username)) {
            is Invalid -> RegisterUserUseCase.Error.ValidationErrors(it.value).left()
            is Valid -> userRepository.register(it.value)
        }
}
