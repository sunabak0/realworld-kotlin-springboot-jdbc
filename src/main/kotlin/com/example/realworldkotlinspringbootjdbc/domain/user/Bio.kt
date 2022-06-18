package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.Option
import arrow.core.Validated
import arrow.core.ValidatedNel
import arrow.core.invalid
import arrow.core.invalidNel
import arrow.core.valid
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface Bio {
    val value: String

    //
    // 実装
    //
    private data class ValidatedBio(override val value: String) : Bio
    private data class BioWithoutValidation(override val value: String) : Bio

    //
    // Factory メソッド
    //
    companion object {
        //
        // Validation 無し
        //
        fun newWithoutValidation(bio: String): Bio = BioWithoutValidation(bio)

        //
        // Validation 有り
        //
        fun new(bio: String?): ValidatedNel<ValidationError, Bio> {
            return when (val result = ValidationError.Required.check(bio)) {
                is Validated.Invalid -> result.value.invalidNel()
                is Validated.Valid -> ValidationError.TooLong.check(result.value)
                    .map { ValidatedBio(result.value) }
            }
        }
    }

    //
    // ドメインルール
    //
    sealed interface ValidationError : MyError.ValidationError {
        override val key: String get() = Bio::class.simpleName.toString()
        //
        // Nullは駄目
        //
        object Required : ValidationError {
            override val message: String get() = "bioを入力してください。"
            fun check(bio: String?): Validated<Required, String> =
                Option.fromNullable(bio).fold(
                    { Required.invalid() },
                    { it.valid() }
                )
        }

        //
        // 長すぎては駄目
        //
        data class TooLong(val bio: String) : ValidationError {
            companion object {
                private const val maximum: Int = 512
                fun check(bio: String): ValidatedNel<ValidationError, Unit> =
                    if (bio.length <= maximum) { Unit.valid() } else { TooLong(bio).invalidNel() }
            }
            override val message: String get() = "bioは${maximum}文字以下にしてください。"
        }
    }
}
