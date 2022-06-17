package com.example.realworldkotlinspringbootjdbc.service

import arrow.core.Either
import arrow.core.Invalid
import arrow.core.Valid
import com.example.realworldkotlinspringbootjdbc.domain.profile.Profile
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface ProfileService {
    fun showProfile(username: String?): Either<ShowProfileError, Profile> = Either.Left(ShowProfileError.NotImplemented)
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
            is Invalid -> Either.Left(ProfileService.ShowProfileError.ValidationErrors(it.value.errors))
            is Valid -> {
                val a = object : Profile {
                    override val username: Username
                        get() = it.value
                    override val bio: Bio
                        get() = object : Bio{override val value: String get() = "hoge-bio"}
                    override val image: String
                        get() = "hoge-image"
                    override val following: Boolean
                        get() = true
                }
                Either.Right(a)
            }
        }
    }
}
