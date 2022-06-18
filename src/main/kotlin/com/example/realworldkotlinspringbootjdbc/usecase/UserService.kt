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
import com.example.realworldkotlinspringbootjdbc.domain.UnregisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Password
import com.example.realworldkotlinspringbootjdbc.infra.UserRepository
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface UserService {
    //
    // ユーザー登録
    //
    fun register(email: String?, password: String?, username: String?): Either<RegisterError, RegisteredUser> = RegisterError.NotImplemented.left()
    sealed interface RegisterError : MyError {
        data class ValidationErrors(override val errors: List<MyError.ValidationError>) : RegisterError, MyError.ValidationErrors
        data class FailedRegister(override val cause: MyError) : RegisterError, MyError.MyErrorWithMyError
        object NotImplemented : RegisterError
    }

    //
    // ログイン
    //
    fun login(email: String?, password: String?): Either<LoginError, RegisteredUser> = LoginError.NotImplemented.left()
    sealed interface LoginError : MyError {
        data class ValidationErrors(override val errors: List<MyError.ValidationError>) : LoginError, MyError.ValidationErrors
        data class Unauthorized(val email: Option<String>) : LoginError, MyError.Basic
        data class UnexpectedError(val email: Option<String>) : LoginError, MyError.Basic
        object NotImplemented : LoginError
    }
}

@Service
class UserServiceImpl(
    val userRepository: UserRepository
) : UserService {
    override fun register(email: String?, password: String?, username: String?): Either<UserService.RegisterError, RegisteredUser> =
        when (val it = UnregisteredUser.new(email, password, username)) {
            is Invalid -> UserService.RegisterError.ValidationErrors(it.value).left()
            is Valid -> userRepository.register(it.value)
        }

    override fun login(email: String?, password: String?): Either<UserService.LoginError, RegisteredUser> {
        val it = Email.new(email).zip(
            Semigroup.nonEmptyList(),
            Password.newForLogin(password)
        ) { a, b -> Pair(a, b) }
        return when (it) {
            //
            // バリデーションエラー
            //
            is Invalid -> UserService.LoginError.ValidationErrors(it.value).left()
            //
            // Email, Password両方ともバリデーションはOK
            //
            is Valid -> {
                val (existedEmail, existedPassword) = it.value
                when (userRepository.findByEmailWithPassword(existedEmail)) {
                    //
                    // 何かしらの失敗
                    //
                    is Left -> UserService.LoginError.NotImplemented.left()
                    //
                    // User は見つかった
                    //
                    is Right -> UserService.LoginError.NotImplemented.left()
                }
            }
        }
    }
}
