package com.example.realworldkotlinspringbootjdbc.domain.comment

import arrow.core.Option
import arrow.core.Validated
import arrow.core.ValidatedNel
import arrow.core.invalid
import arrow.core.invalidNel
import arrow.core.valid
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface CommentId {
    val value: Int

    /**
     * 実装
     */
    private data class ValidatedCommentId(override val value: Int) : CommentId
    private data class CommentIdWithoutValidation(override val value: Int) : CommentId

    /**
     * Factory メソッド
     */
    companion object {
        fun newWithoutValidation(id: Int): CommentId = CommentIdWithoutValidation(id)

        fun new(id: Int?): ValidatedNel<ValidationError, CommentId> {
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

            fun check(id: Int?): Validated<Required, Int> =
                Option.fromNullable(id).fold(
                    { Required.invalid() },
                    { it.valid() }
                )
        }

        /**
         * 自然数（0より大きい整数）
         */
        data class MustBeNaturalNumber(val id: Int) : ValidationError {
            companion object {
                fun check(id: Int): ValidatedNel<ValidationError, Int> =
                    if (id > 0) {
                        id.valid()
                    } else {
                        MustBeNaturalNumber(id).invalidNel()
                    }
            }

            override val message: String get() = "id は0より大きい整数にしてください"
        }
    }
}
