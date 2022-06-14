package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.Option
import arrow.core.Validated
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface Bio {
    val value: String
    data class ValidationErrors(override val errors: List<ValidationError>) : MyError.ValidationErrors
    sealed interface ValidationError : MyError.ValidationError {
        override val key: String get() = "bio"
        object Required : ValidationError {
            override val message: String get() = "bioを入力してください。"
            fun check(bio: String?): Validated<Required, String> =
                Option.fromNullable(bio).fold(
                    { Validated.Invalid(Required) },
                    { Validated.Valid(it) }
                )
        }

        data class TooLong(val bio: String) : ValidationError {
            companion object {
                private const val maximum: Int = 512
                fun check(bio: String): Validated<TooLong, String> =
                    if (bio.length <= maximum) {
                        Validated.Valid(bio)
                    } else {
                        Validated.Invalid(TooLong(bio))
                    }
            }

            override val message: String get() = "bioは${maximum}文字以下にしてください。"
        }
    }

    companion object {
        fun new(bio: String?): Validated<ValidationErrors, Bio> {
            val existedBio = when (val it = ValidationError.Required.check(bio)) {
                is Validated.Invalid -> { return Validated.Invalid(ValidationErrors(listOf(it.value))) }
                is Validated.Valid -> it.value
            }
            val errors = mutableListOf<ValidationError>()
            when (val it = ValidationError.TooLong.check(existedBio)) {
                is Validated.Invalid -> { errors.add(it.value) }
                is Validated.Valid -> {}
            }
            return if (errors.size == 0) { Validated.Valid(BioImpl(existedBio)) } else {
                Validated.Invalid(
                    ValidationErrors(
                        errors
                    )
                )
            }
        }
    }

    private data class BioImpl(override val value: String) : Bio
}
