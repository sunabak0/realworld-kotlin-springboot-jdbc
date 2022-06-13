package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.Option
import arrow.core.Validated
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface Email {
    val value: String
    data class ValidationErrors(override val errors: List<ValidationError>) : MyError.ValidationErrors
    sealed interface ValidationError : MyError.ValidationError {
        object Required : ValidationError {
            override val message: String get() = "必須項目です"
            fun check(password: String?): Validated<Required, String> =
                Option.fromNullable(password).fold(
                    { Validated.Invalid(Required) },
                    { Validated.Valid(it) }
                )
        }
        data class InvalidFormat(val email: String) : ValidationError {
            override val message: String get() = "不正です"
            companion object {
                // emailPattern
                // https://android.googlesource.com/platform/frameworks/base/+/81aa097/core/java/android/util/Patterns.java#146
                private const val emailPattern = """[a-zA-Z0-9+._%\-+]{1,256}""" +
                    """\@""" +
                    """[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}""" +
                    "(" +
                    """\.""" +
                    """[a-zA-Z0-9][a-zA-Z0-9\-]{0,25}""" +
                    ")+"
                fun check(email: String): Validated<InvalidFormat, String> =
                    if (email.matches(emailPattern.toRegex())) { Validated.Valid(email) } else { Validated.Invalid(InvalidFormat(email)) }
            }
        }
    }

    companion object {
        fun new(email: String?): Validated<ValidationErrors, Email> {
            val existedEmail = when (val it = ValidationError.Required.check(email)) {
                is Validated.Invalid -> { return Validated.Invalid(ValidationErrors(listOf(it.value))) }
                is Validated.Valid -> it.value
            }
            val errors = mutableListOf<ValidationError>()
            when (val it = ValidationError.InvalidFormat.check(existedEmail)) {
                is Validated.Invalid -> { errors.add(it.value) }
                is Validated.Valid -> {}
            }
            return if (errors.size == 0) { Validated.Valid(EmailImpl(existedEmail)) } else { Validated.Invalid(ValidationErrors(errors)) }
        }
    }

    private data class EmailImpl(override val value: String) : Email
}
