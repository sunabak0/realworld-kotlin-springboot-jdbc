package com.example.realworldkotlinspringbootjdbc.domain.article

import arrow.core.Option
import arrow.core.Validated
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.ValidatedNel
import arrow.core.invalidNel
import arrow.core.valid
import arrow.core.zip
import arrow.typeclasses.Semigroup
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface Tag {
    val value: String

    /**
     * 実装
     */
    private data class ValidatedTag(override val value: String) : Tag
    private data class TagWithoutValidation(override val value: String) : Tag

    /**
     * Factory メソッド
     */
    companion object {
        /**
         * Validation 無し
         */
        fun newWithoutValidation(tag: String): Tag = TagWithoutValidation(tag)

        /**
         * Validation 有り
         */
        fun new(tag: String?): ValidatedNel<ValidationError, Tag> =
            when (val result = ValidationError.Required.check(tag)) {
                is Invalid -> result.value.invalidNel()
                is Valid -> {
                    ValidationError.TooShort.check(result.value).zip(
                        Semigroup.nonEmptyList(),
                        ValidationError.TooLong.check(result.value)
                    ) { _, _ -> ValidatedTag(result.value) }
                }
            }
    }

    /**
     * ドメインルール
     */
    sealed interface ValidationError : MyError.ValidationError {
        override val key: String get() = Tag::class.simpleName.toString()

        /**
         * Nullは駄目
         */
        object Required : ValidationError {
            override val message: String get() = "tagを入力してください。"
            fun check(tag: String?): Validated<Required, String> =
                Option.fromNullable(tag).fold(
                    { Invalid(Required) },
                    { Valid(it) }
                )
        }

        /**
         * 短すぎては駄目
         */
        data class TooShort(val tag: String) : ValidationError {
            companion object {
                private const val minimum: Int = 1
                fun check(tag: String): ValidatedNel<TooShort, Unit> =
                    if (minimum <= tag.length) {
                        Unit.valid()
                    } else {
                        TooShort(tag).invalidNel()
                    }
            }
            override val message: String get() = "tagは${minimum}文字以上にしてください。"
        }

        /**
         * 長すぎては駄目
         */
        data class TooLong(val tag: String) : ValidationError {
            companion object {
                private const val maximum: Int = 16
                fun check(tag: String): ValidatedNel<TooLong, Unit> =
                    if (tag.length <= maximum) {
                        Unit.valid()
                    } else {
                        TooLong(tag).invalidNel()
                    }
            }
            override val message: String get() = "tagは${maximum}文字以下にしてください。"
        }
    }
}
