package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Validated
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface RegisteredUser {
    val userId: UserId
    val email: Email
    val username: Username
    val bio: Bio
    val image: Image

    data class ValidationErrors(override val errors: List<MyError.ValidationError>) : MyError.ValidationErrors
    companion object {
        fun new(
            userId: Int,
            email: String?,
            username: String?,
            bio: String?,
            image: String?
        ): Validated<ValidationErrors, RegisteredUser> {
            // TODO: More smart
            val errors = mutableListOf<MyError.ValidationError>()
            val validatedEmail = Email.new(email)
            val validatedUsername = Username.new(username)
            val validatedBio = Bio.new(bio)
            val validatedImage = Image.new(image)
            when (validatedEmail) {
                is Validated.Valid -> when (validatedUsername) {
                    is Validated.Valid -> when (validatedBio) {
                        is Validated.Valid -> when (validatedImage) {
                            is Validated.Valid -> {
                                val user = RegisteredUserImpl(
                                    UserId(userId),
                                    validatedEmail.value,
                                    validatedUsername.value,
                                    validatedBio.value,
                                    validatedImage.value,
                                )
                                return Validated.Valid(user)
                            }
                            is Validated.Invalid -> {}
                        }
                        is Validated.Invalid -> {}
                    }
                    is Validated.Invalid -> {}
                }
                is Validated.Invalid -> {}
            }
            when (validatedEmail) {
                is Validated.Invalid -> {
                    errors.addAll(validatedEmail.value.errors)
                }
                is Validated.Valid -> {}
            }
            when (validatedUsername) {
                is Validated.Invalid -> {
                    errors.addAll(validatedUsername.value.errors)
                }
                is Validated.Valid -> {}
            }
            when (validatedBio) {
                is Validated.Invalid -> {
                    errors.addAll(validatedBio.value.errors)
                }
                is Validated.Valid -> {}
            }
            when (validatedImage) {
                is Validated.Invalid -> {
                    errors.addAll(validatedImage.value.errors)
                }
                is Validated.Valid -> {}
            }
            return Validated.Invalid(ValidationErrors(errors))
        }
    }

    private data class RegisteredUserImpl(
        override val userId: UserId,
        override val email: Email,
        override val username: Username,
        override val bio: Bio,
        override val image: Image,
    ) : RegisteredUser
}
