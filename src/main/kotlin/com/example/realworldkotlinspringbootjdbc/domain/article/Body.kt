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
 * 「作成済記事の本文」の値オブジェクト
 */
interface Body {
    val value: String

    /**
     * 実装
     */
    private data class ValidatedBody(override val value: String) : Body
    private data class BodyWithoutValidation(override val value: String) : Body

    /**
     * Factory メソッド
     */
    companion object {
        /**
         * Validation 無し
         */
        fun newWithoutValidation(body: String): Body = BodyWithoutValidation(body)

        /**
         * Validation あり
         *
         * @param body
         * @return バリデーションエラー or CreatedArticle（作成済記事）の本文
         */
        fun new(body: String?): ValidatedNel<ValidationError, Body> {
            return when (val result = ValidationError.Required.check(body)) {
                is Invalid -> result.value.invalidNel()
                is Valid -> ValidationError.TooLong.check(result.value)
                    .map { ValidatedBody(result.value) }
            }
        }
    }

    /**
     * ドメインルール
     */
    sealed interface ValidationError : MyError.ValidationError {
        override val key: String get() = Body::class.simpleName.toString()

        /**
         * Null は駄目
         */
        object Required : ValidationError {
            override val message: String get() = "body を入力してください"
            fun check(body: String?): Validated<Required, String> =
                Option.fromNullable(body).fold(
                    { Invalid(Required) },
                    { Valid(it) }
                )
        }

        /**
         * 長すぎては駄目
         */
        data class TooLong(val body: String) : ValidationError {
            companion object {
                private const val maximum: Int = 1024
                fun check(body: String): ValidatedNel<TooLong, Unit> =
                    if (body.length <= maximum) {
                        Unit.valid()
                    } else {
                        TooLong(body).invalidNel()
                    }
            }

            override val message: String get() = "body は${maximum}文字以下にしてください。"
        }
    }
}
