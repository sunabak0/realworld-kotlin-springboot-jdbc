package com.example.realworldkotlinspringbootjdbc.domain.article

import arrow.core.Option
import arrow.core.Validated
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.ValidatedNel
import arrow.core.invalidNel
import arrow.core.valid
import com.example.realworldkotlinspringbootjdbc.util.MyError

/**
 * 「作成済記事の概要」の値オブジェクト
 */
interface Description {
    val value: String

    /**
     * 実装
     */
    private data class ValidatedDescription(override val value: String) : Description
    private data class DescriptionWithoutValidation(override val value: String) : Description

    /**
     * Factory メソッド
     */
    companion object {
        /**
         * Validation 無し
         */
        fun newWithoutValidation(description: String): Description = DescriptionWithoutValidation(description)

        /**
         * Validation 有り
         *
         * @param description
         * @return バリデーションエラー or 作成済記事の概要
         */
        fun new(description: String?): ValidatedNel<ValidationError, Description> {
            return when (val result = ValidationError.Required.check(description)) {
                is Invalid -> result.value.invalidNel()
                is Valid -> ValidationError.TooLong.check(result.value).map { ValidatedDescription(result.value) }
            }
        }
    }

    /**
     * ドメインルール
     */
    sealed interface ValidationError : MyError.ValidationError {
        override val key: String get() = Description::class.simpleName.toString()

        /**
         * Null は駄目
         */
        object Required : ValidationError {
            override val message: String get() = "title を入力してください"
            fun check(description: String?): Validated<Required, String> =
                Option.fromNullable(description).fold(
                    { Invalid(Required) },
                    { Valid(it) }
                )
        }

        /**
         * 長すぎては駄目
         */
        data class TooLong(val description: String) : ValidationError {
            companion object {
                private const val maximum: Int = 64
                fun check(description: String): ValidatedNel<TooLong, Unit> =
                    if (description.length <= maximum) {
                        Unit.valid()
                    } else {
                        TooLong(description).invalidNel()
                    }
            }

            override val message: String get() = "descriptionは${maximum}文字以下にしてください。"
        }
    }
}
