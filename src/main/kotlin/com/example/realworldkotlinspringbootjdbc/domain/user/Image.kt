package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.Option
import arrow.core.Validated
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface Image {
    val value: String
    data class ValidationErrors(override val errors: List<ValidationError>) : MyError.ValidationErrors
    sealed interface ValidationError : MyError.ValidationError {
        override val key: String get() = "image"
        object Required : ValidationError {
            override val message: String get() = "imageを入力してください。"
            fun check(image: String?): Validated<Required, String> =
                Option.fromNullable(image).fold(
                    { Validated.Invalid(Required) },
                    { Validated.Valid(it) }
                )
        }

        data class TooLong(val image: String) : ValidationError {
            companion object {
                private const val maximum: Int = 512
                fun check(image: String): Validated<TooLong, String> =
                    if (image.length <= maximum) {
                        Validated.Valid(image)
                    } else {
                        Validated.Invalid(TooLong(image))
                    }
            }

            override val message: String get() = "imageは${maximum}文字以下にしてください。"
        }
    }

    companion object {
        fun new(image: String?): Validated<ValidationErrors, Image> {
            val existedImage = when (val it = ValidationError.Required.check(image)) {
                is Validated.Invalid -> { return Validated.Invalid(ValidationErrors(listOf(it.value))) }
                is Validated.Valid -> it.value
            }
            val errors = mutableListOf<ValidationError>()
            when (val it = ValidationError.TooLong.check(existedImage)) {
                is Validated.Invalid -> { errors.add(it.value) }
                is Validated.Valid -> {}
            }
            return if (errors.size == 0) { Validated.Valid(ImageImpl(existedImage)) } else {
                Validated.Invalid(
                    ValidationErrors(
                        errors
                    )
                )
            }
        }
    }

    private data class ImageImpl(override val value: String) : Image
}
