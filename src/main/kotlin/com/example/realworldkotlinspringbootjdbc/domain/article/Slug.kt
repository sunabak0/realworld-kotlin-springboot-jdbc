package com.example.realworldkotlinspringbootjdbc.domain.article

import arrow.core.Option
import arrow.core.Validated
import arrow.core.ValidatedNel
import arrow.core.invalidNel
import arrow.core.valid
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface Slug {
    val value: String

    /**
     * 実装
     */
    private data class ValidatedSlug(override val value: String) : Slug
    private data class SlugWithoutValidation(override val value: String) : Slug

    /**
     * Factory メソッド
     */
    companion object {
        /**
         * Validation 無し
         */
        fun newWithoutValidation(slug: String): Slug = Slug.SlugWithoutValidation(slug)

        /**
         * Validation 有り
         */
        fun new(slug: String?): ValidatedNel<ValidationError, Slug> {
            return when (val result = ValidationError.Required.check(slug)) {
                is Validated.Invalid -> result.value.invalidNel()
                is Validated.Valid -> ValidationError.TooLong.check(result.value)
                    .map { ValidatedSlug(result.value) }
            }
        }
    }

    /**
     * ドメインルール
     */
    sealed interface ValidationError : MyError.ValidationError {
        override val key: String get() = Slug::class.simpleName.toString()
        /**
         * Nullは駄目
         */
        object Required : ValidationError {
            override val message: String get() = "slugを入力してください。"
            fun check(slug: String?): Validated<Required, String> =
                Option.fromNullable(slug).fold(
                    { Validated.Invalid(Required) },
                    { Validated.Valid(it) }
                )
        }

        /**
         * 長すぎては駄目
         */
        data class TooLong(val slug: String) : ValidationError {
            companion object {
                private const val maximum: Int = 32
                fun check(slug: String): ValidatedNel<TooLong, Unit> =
                    if (slug.length <= maximum) { Unit.valid() } else { TooLong(slug).invalidNel() }
            }
            override val message: String get() = "slugは${maximum}文字以下にしてください。"
        }
    }
}
