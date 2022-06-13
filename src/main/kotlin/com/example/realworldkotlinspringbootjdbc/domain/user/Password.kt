package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.Option
import arrow.core.Validated
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface Password {
    val value: String
    data class ValidationErrors(override val errors: List<ValidationError>) : MyError.ValidationErrors<ValidationError>
    sealed interface ValidationError : MyError.ValidationError {
        object Required : ValidationError {
            override val message: String get() = "必須項目です"
            fun check(password: String?): Validated<Required, String> =
                Option.fromNullable(password).fold(
                    { Validated.Invalid(Required) },
                    { Validated.Valid(it) }
                )
        }
        data class TooShort(val password: String) : ValidationError {
            companion object {
                private const val minimum: Int = 8
                fun check(password: String): Validated<TooShort, String> =
                    if (minimum <= password.length) {
                        Validated.Valid(password)
                    } else {
                        Validated.Invalid(TooShort(password))
                    }
            }
            override val message: String get() = "${minimum}文字以上にしてください"
        }
        data class TooLong(val password: String) : ValidationError {
            companion object {
                private const val maximum: Int = 32
                fun check(password: String): Validated<TooLong, String> =
                    if (password.length <= maximum) {
                        Validated.Valid(password)
                    } else {
                        Validated.Invalid(TooLong(password))
                    }
            }
            override val message: String get() = "${maximum}文字以下にしてください"
        }
    }

    companion object {
        fun new(password: String?): Validated<ValidationErrors, Password> {
            val existedPassword = when (val it = ValidationError.Required.check(password)) {
                is Validated.Invalid -> { return Validated.Invalid(ValidationErrors(listOf(it.value))) }
                is Validated.Valid -> it.value
            }
            val errors = mutableListOf<ValidationError>()
            when (val it = ValidationError.TooShort.check(existedPassword)) {
                is Validated.Invalid -> { errors.add(it.value) }
                is Validated.Valid -> {}
            }
            when (val it = ValidationError.TooLong.check(existedPassword)) {
                is Validated.Invalid -> { errors.add(it.value) }
                is Validated.Valid -> {}
            }
            return if (errors.size == 0) { Validated.Valid(PasswordImpl(existedPassword)) } else {
                Validated.Invalid(
                    ValidationErrors(
                        errors
                    )
                )
            }
        }
    }

    private data class PasswordImpl(override val value: String) : Password
}
