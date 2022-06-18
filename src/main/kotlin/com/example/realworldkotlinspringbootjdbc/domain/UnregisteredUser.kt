package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Validated
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Password
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError
import com.example.realworldkotlinspringbootjdbc.util.MyError.ValidationError

interface UnregisteredUser {
    val email: Email
    val password: Password
    val username: Username

    data class ValidationErrors(override val errors: List<ValidationError>) : MyError.ValidationErrors
    companion object {
        fun new(
            email: String?,
            password: String?,
            username: String?,
        ): Validated<ValidationErrors, UnregisteredUser> {
            // TODO: More smart
            val errors = mutableListOf<ValidationError>()
            val validatedEmail = Email.new(email)
            val validatedPassword = Password.new(password)
            val validatedUsername = Username.new(username)
            when (validatedEmail) {
                is Validated.Valid -> when (validatedPassword) {
                    is Validated.Valid -> when (validatedUsername) {
                        is Validated.Valid -> {
                            val user = UnregisteredUserImpl(
                                validatedEmail.value,
                                validatedPassword.value,
                                validatedUsername.value,
                            )
                            return Validated.Valid(user)
                        }
                        is Validated.Invalid -> {}
                    }
                    is Validated.Invalid -> {}
                }
                is Validated.Invalid -> {}
            }
            // TODO: Too Bad
            when (validatedEmail) {
                is Validated.Invalid -> { errors.addAll(validatedEmail.value.errors) }
                is Validated.Valid -> {}
            }
            when (validatedPassword) {
                is Validated.Invalid -> { errors.addAll(validatedPassword.value) }
                is Validated.Valid -> {}
            }
            when (validatedUsername) {
                is Validated.Invalid -> { errors.addAll(validatedUsername.value) }
                is Validated.Valid -> {}
            }
            return Validated.Invalid(ValidationErrors(errors))
        }
    }

    private data class UnregisteredUserImpl(
        override val email: Email,
        override val password: Password,
        override val username: Username,
    ) : UnregisteredUser
}
