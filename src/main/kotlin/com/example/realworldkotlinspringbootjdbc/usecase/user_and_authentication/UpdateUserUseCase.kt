package com.example.realworldkotlinspringbootjdbc.usecase.user_and_authentication

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UpdatableRegisteredUser
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface UpdateUserUseCase {
    fun execute(
        currentUser: RegisteredUser,
        email: String?,
        username: String?,
        bio: String?,
        image: String?,
    ): Either<Error, RegisteredUser> = TODO()

    sealed interface Error : MyError {
        data class InvalidAttributesForUpdateUser(val currentUser: RegisteredUser, val errors: NonEmptyList<MyError.ValidationError>) : Error, MyError.Basic
        data class NoChange(val currentUser: RegisteredUser) : Error, MyError.Basic
        data class Unexpected(val currentUser: RegisteredUser, override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class UpdateUserUseCaseImpl : UpdateUserUseCase {
    override fun execute(
        currentUser: RegisteredUser,
        email: String?,
        username: String?,
        bio: String?,
        image: String?
    ): Either<UpdateUserUseCase.Error, RegisteredUser> {
        when (val updatableRegisteredUser = UpdatableRegisteredUser.new(currentUser, email, username, bio, image)) {
            /**
             * 更新可能かも
             */
            is Valid -> {
                TODO()
                // val newRegisteredUser = RegisteredUser.newWithoutValidation(
                //    currentUser.userId,
                //    updatableRegisteredUser.value.email.getOrElse { currentUser.email },
                //    updatableRegisteredUser.value.username.getOrElse { currentUser.username },
                //    updatableRegisteredUser.value.bio.getOrElse { currentUser.bio },
                //    updatableRegisteredUser.value.image.getOrElse { currentUser.image }
                // )
            }
            /**
             *
             */
            is Invalid -> TODO()
        }

        TODO("Not yet implemented")
    }
}
