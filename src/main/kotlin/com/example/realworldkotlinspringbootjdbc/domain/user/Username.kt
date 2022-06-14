package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.Option
import arrow.core.Validated
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface Username {
    val value: String
    data class ValidationErrors(override val errors: List<ValidationError>) : MyError.ValidationErrors
    sealed interface ValidationError : MyError.ValidationError {
        override val key: String get() = "username"
        object Required : ValidationError {
            override val message: String get() = "ユーザ名を入力してください。"
            fun check(username: String?): Validated<Required, String> =
                Option.fromNullable(username).fold(
                    { Validated.Invalid(Required) },
                    { Validated.Valid(it) }
                )
        }

        data class TooShort(val username: String) : ValidationError {
            companion object {
                private const val minimum: Int = 4
                fun check(username: String): Validated<TooShort, String> =
                    if (minimum <= username.length) {
                        Validated.Valid(username)
                    } else {
                        Validated.Invalid(TooShort(username))
                    }
            }

            override val message: String get() = "ユーザー名は${minimum}文字以上にしてください。"
        }

        data class TooLong(val username: String) : ValidationError {
            companion object {
                private const val maximum: Int = 32
                fun check(username: String): Validated<TooLong, String> =
                    if (username.length <= maximum) {
                        Validated.Valid(username)
                    } else {
                        Validated.Invalid(TooLong(username))
                    }
            }

            override val message: String get() = "ユーザー名は${maximum}文字以下にしてください。"
        }
    }

    companion object {
        fun new(username: String?): Validated<ValidationErrors, Username> {
            val existedUsername = when (val it = ValidationError.Required.check(username)) {
                is Validated.Invalid -> { return Validated.Invalid(ValidationErrors(listOf(it.value))) }
                is Validated.Valid -> it.value
            }
            val errors = mutableListOf<ValidationError>()
            when (val it = ValidationError.TooShort.check(existedUsername)) {
                is Validated.Invalid -> { errors.add(it.value) }
                is Validated.Valid -> {}
            }
            when (val it = ValidationError.TooLong.check(existedUsername)) {
                is Validated.Invalid -> { errors.add(it.value) }
                is Validated.Valid -> {}
            }
            return if (errors.size == 0) { Validated.Valid(UsernameImpl(existedUsername)) } else {
                Validated.Invalid(
                    ValidationErrors(
                        errors
                    )
                )
            }
        }
    }

    private data class UsernameImpl(override val value: String) : Username
}
