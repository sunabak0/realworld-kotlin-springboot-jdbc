package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.Option
import arrow.core.Validated
import arrow.core.ValidatedNel
import arrow.core.invalid
import arrow.core.invalidNel
import arrow.core.valid
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface Image {
    val value: String

    //
    // 実装
    //
    private data class ValidatedImage(override val value: String) : Image
    private data class ImageWithoutValidation(override val value: String) : Image

    //
    // Factory メソッド
    //
    companion object {
        //
        // Validation 無し
        //
        fun newWithoutValidation(image: String): Image = ImageWithoutValidation(image)

        //
        // Validation 有り
        //
        fun new(image: String?): ValidatedNel<ValidationError, Image> {
            return when (val result = ValidationError.Required.check(image)) {
                is Validated.Invalid -> result.value.invalidNel()
                is Validated.Valid -> ValidationError.TooLong.check(result.value)
                    .map { ValidatedImage(result.value) }
            }
        }
    }

    //
    // ドメインルール
    //
    sealed interface ValidationError : MyError.ValidationError {
        override val key: String get() = Image::class.simpleName.toString()
        //
        // Nullは駄目
        //
        object Required : ValidationError {
            override val message: String get() = "imageを入力してください。"
            fun check(image: String?): Validated<Required, String> =
                Option.fromNullable(image).fold(
                    { Required.invalid() },
                    { it.valid() }
                )
        }

        //
        // 長すぎては駄目
        //
        data class TooLong(val image: String) : ValidationError {
            companion object {
                private const val maximum: Int = 512
                fun check(image: String): ValidatedNel<TooLong, Unit> =
                    if (image.length <= maximum) { Unit.valid() } else { TooLong(image).invalidNel() }
            }
            override val message: String get() = "imageは${maximum}文字以下にしてください。"
        }
    }
}
