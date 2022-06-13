package com.example.realworldkotlinspringbootjdbc.service

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface UserService {
    fun register(email: String?, password: String?, username: String?): Either<RegisterError, RegisteredUser> = Either.Left(RegisterError.NotImplemented)
    sealed interface RegisterError : MyError {
        data class ValidationErrors(override val errors: List<MyError.ValidationError>) : RegisterError, MyError.ValidationErrors
        data class FailedRegister(override val cause: MyError) : RegisterError, MyError.MyErrorWithMyError
        object NotImplemented : RegisterError
    }
}

@Service
class UserServiceImpl : UserService {
    override fun register(email: String?, password: String?, username: String?): Either<UserService.RegisterError, RegisteredUser> {
        return Either.Left(UserService.RegisterError.NotImplemented)
    }
}
