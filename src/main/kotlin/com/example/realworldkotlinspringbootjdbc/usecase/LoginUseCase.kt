package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Option
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
import arrow.core.zip
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
        data class Unauthorized(val email: Option<String>) : Error, MyError.Basic
        data class UnexpectedError(val email: Option<String>) : Error, MyError.Basic
        object NotImplemented : Error
    }
}

@Service
class LoginUseCaseImpl(
    val userRepository: UserRepository
) : LoginUseCase {
    override fun execute(email: String?, password: String?): Either<LoginUseCase.Error, RegisteredUser> {
        val it = Email.new(email).zip(
            Semigroup.nonEmptyList(),
            Password.newForLogin(password)
        ) { a, b -> Pair(a, b) }
        return when (it) {
            //
            // バリデーションエラー
            //
            is Invalid -> LoginUseCase.Error.ValidationErrors(it.value).left()
            //
            // Email, Password両方ともバリデーションはOK
            //
            is Valid -> {
                val (existedEmail, existedPassword) = it.value
                when (userRepository.findByEmailWithPassword(existedEmail)) {
                    //
                    // 何かしらの失敗
                    //
                    is Left -> LoginUseCase.Error.NotImplemented.left()
                    //
                    // User は見つかった
                    //
                    is Right -> LoginUseCase.Error.NotImplemented.left()
                }
            }
        }
    }
}
