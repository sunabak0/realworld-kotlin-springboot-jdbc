package com.example.realworldkotlinspringbootjdbc.domain.article

import arrow.core.Option
import arrow.core.Validated
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.ValidatedNel
import arrow.core.invalidNel
import arrow.core.valid
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface Title {
    val value: String

    //
    // 実装
    //
    private data class ValidatedTitle(override val value: String) : Title
    private data class TitleWithoutValidation(override val value: String) : Title

    //
    // Factory メソッド
    //
    companion object {
        //
        // Validation 無し
        //
        fun newWithoutValidation(title: String): Title = TitleWithoutValidation(title)

        //
        // Validation 有り
        //
        fun new(title: String?): ValidatedNel<ValidationError, Title> {
            return when (val result = ValidationError.Required.check(title)) {
                is Invalid -> result.value.invalidNel()
                is Valid -> ValidationError.TooLong.check(result.value)
                    .map { ValidatedTitle(result.value) }
            }
        }
    }

    //
    // ドメインルール
    //
    sealed interface ValidationError : MyError.ValidationError {
        override val key: String get() = Title::class.simpleName.toString()
        //
        // Nullは駄目
        //
        object Required : ValidationError {
            override val message: String get() = "titleを入力してください。"
            fun check(title: String?): Validated<Required, String> =
                Option.fromNullable(title).fold(
                    { Invalid(Required) },
                    { Valid(it) }
                )
        }

        //
        // 長すぎては駄目
        //
        data class TooLong(val title: String) : ValidationError {
            companion object {
                private const val maximum: Int = 32
                fun check(title: String): ValidatedNel<TooLong, Unit> =
                    if (title.length <= maximum) { Unit.valid() } else { TooLong(title).invalidNel() }
            }
            override val message: String get() = "titleは${maximum}文字以下にしてください。"
        }
    }
}
