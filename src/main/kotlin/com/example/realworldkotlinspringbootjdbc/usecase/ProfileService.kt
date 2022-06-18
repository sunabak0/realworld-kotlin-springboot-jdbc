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

interface ProfileService {
    fun showProfile(username: String?): Either<ShowProfileError, Profile> = Left(ShowProfileError.NotImplemented)
    sealed interface ShowProfileError : MyError {
        data class ValidationErrors(override val errors: List<MyError.ValidationError>) :
            ShowProfileError,
            MyError.ValidationErrors

        data class FailedShow(override val cause: MyError) : ShowProfileError, MyError.MyErrorWithMyError
        data class NotFound(override val cause: MyError) : ShowProfileError, MyError.MyErrorWithMyError
        object NotImplemented : ShowProfileError
    }
}

@Service
class ProfileServiceImpl() : ProfileService {
    override fun showProfile(username: String?): Either<ProfileService.ShowProfileError, Profile> {
        return when (val it = Username.new(username)) {
            is Invalid -> ProfileService.ShowProfileError.ValidationErrors(it.value).left()
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
