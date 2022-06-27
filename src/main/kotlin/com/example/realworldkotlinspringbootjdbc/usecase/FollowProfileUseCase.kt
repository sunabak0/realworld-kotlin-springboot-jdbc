package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.Validated
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.Profile
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface FollowProfileUseCase {
    fun execute(username: String?): Either<Error, Profile> = TODO()
    sealed interface Error : MyError {
        data class InvalidUserName(override val errors: List<MyError.ValidationError>) :
            Error,
            MyError.ValidationErrors

        data class NotFound(override val cause: MyError) : Error, MyError.MyErrorWithMyError
        data class Unexpected(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class FollowProfileUseCaseImpl() : FollowProfileUseCase {
    override fun execute(username: String?): Either<FollowProfileUseCase.Error, Profile> {
        return when (val it = Username.new(username)) {
            is Validated.Invalid -> FollowProfileUseCase.Error.InvalidUserName(it.value).left()
            is Validated.Valid -> TODO()
        }
    }
}
