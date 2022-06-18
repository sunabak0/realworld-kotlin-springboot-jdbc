package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Profile
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface ShowProfileUseCase {
    fun execute(username: String?): Either<Error, Profile> = Left(Error.NotImplemented)
    sealed interface Error : MyError {
        data class ValidationErrors(override val errors: List<MyError.ValidationError>) :
            Error,
            MyError.ValidationErrors

        data class FailedShow(override val cause: MyError) : Error, MyError.MyErrorWithMyError
        data class NotFound(override val cause: MyError) : Error, MyError.MyErrorWithMyError
        object NotImplemented : Error
    }
}

@Service
class ShowProfileUseCaseImpl() : ShowProfileUseCase {
    override fun execute(username: String?): Either<ShowProfileUseCase.Error, Profile> {
        return when (val it = Username.new(username)) {
            is Invalid -> ShowProfileUseCase.Error.ValidationErrors(it.value).left()
            is Valid -> {
                val a = object : Profile {
                    override val username: Username
                        get() = it.value
                    override val bio: Bio
                        get() = object : Bio { override val value: String get() = "hoge-bio" }
                    override val image: String
                        get() = "hoge-image"
                    override val following: Boolean
                        get() = true
                }
                a.right()
            }
        }
    }
}
