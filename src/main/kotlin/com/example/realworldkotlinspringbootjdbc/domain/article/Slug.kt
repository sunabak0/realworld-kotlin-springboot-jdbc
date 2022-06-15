package com.example.realworldkotlinspringbootjdbc.domain.article

import arrow.core.Option
import arrow.core.Validated
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface Slug {
    val value: String

    data class ValidationErrors(override val errors: List<ValidationError>) : MyError.ValidationErrors
    sealed interface ValidationError : MyError.ValidationError {
        override val key: String get() = "slug"

        object Required : ValidationError {
            override val message: String get() = "slugを入力してください。"
            fun check(slug: String?): Validated<Required, String> =
                Option.fromNullable(slug).fold(
                    { Validated.Invalid(Required) },
                    { Validated.Valid(it) }
                )
        }

        data class TooLong(val slug: String) : ValidationError {
            companion object {
                private const val maximum: Int = 32
                fun check(slug: String): Validated<TooLong, String> =
                    if (slug.length <= maximum) {
                        Validated.Valid(slug)
                    } else {
                        Validated.Invalid(TooLong(slug))
                    }
            }

            override val message: String get() = "slugは${maximum}文字以下にしてください。"
        }
    }

    companion object {
        fun new(slug: String?): Validated<ValidationErrors, Slug> {
            val existedSlug = when (val it = ValidationError.Required.check(slug)) {
                is Validated.Invalid -> {
                    return Validated.Invalid(ValidationErrors(listOf(it.value)))
                }
                is Validated.Valid -> it.value
            }
            val errors = mutableListOf<ValidationError>()
            when (val it = ValidationError.TooLong.check(existedSlug)) {
                is Validated.Invalid -> {
                    errors.add(it.value)
                }
                is Validated.Valid -> {}
            }
            return if (errors.size == 0) {
                Validated.Valid(SlugImpl(existedSlug))
            } else {
                Validated.Invalid(
                    ValidationErrors(
                        errors
                    )
                )
            }
        }
    }

    private data class SlugImpl(override val value: String) : Slug
}
