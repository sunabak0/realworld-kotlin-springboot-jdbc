package com.example.realworldkotlinspringbootjdbc.domain.comment

import arrow.core.Option
import arrow.core.Validated
import arrow.core.ValidatedNel
import arrow.core.invalid
import arrow.core.invalidNel
import arrow.core.valid
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface CommentId {
    val value: String

    /**
     * 実装
     */
    private data class ValidatedCommentId(override val value: String) : CommentId
    private data class CommentIdWithoutValidation(override val value: String) : CommentId

    /**
     * Factory メソッド
     */
    companion object {
        fun newWithoutValidation(id: String): CommentId = CommentIdWithoutValidation(id)

        fun new(id: String?): ValidatedNel<ValidationError, CommentId> {
            return when (val result = ValidationError.Required.check(id)) {
                is Validated.Invalid -> result.value.invalidNel()
                is Validated.Valid -> ValidationError.MustBeNaturalNumber.check(result.value)
                    .map { ValidatedCommentId(result.value) }
            }
        }
    }

    /**
     * ドメインルール
     */
    sealed interface ValidationError : MyError.ValidationError {
        override val key: String get() = CommentId::class.simpleName.toString()

        /**
         * 必須
         */
        object Required : ValidationError {
            override val message: String
                get() = "id を入力してください"

            fun check(id: String?): Validated<Required, String> =
                Option.fromNullable(id).fold(
                    { Required.invalid() },
                    { it.valid() }
                )
        }

        /**
         * 自然数（0より大きい整数）
         */
        data class MustBeNaturalNumber(val id: String) : ValidationError {
            companion object {
                fun check(id: String): ValidatedNel<ValidationError, String> =
                    when (val result = id.toUIntOrNull()) {
                        is UInt -> if (result > 0.toUInt()) {
                            result.toString().valid()
                        } else {
                            MustBeNaturalNumber(id).invalidNel()
                        }
                        else -> MustBeNaturalNumber(id).invalidNel()
                    }
            }

            override val message: String get() = "id は0より大きい整数にしてください"
        }
    }
}
