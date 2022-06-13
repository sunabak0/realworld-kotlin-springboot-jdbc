package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.Option
import arrow.core.Validated
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface Email {
    val value: String
    data class ValidationErrors(override val errors: List<ValidationError>) : MyError.ValidationErrors<ValidationError>
    sealed interface ValidationError : MyError.ValidationError {
        object Required : Password.ValidationError {
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
}
