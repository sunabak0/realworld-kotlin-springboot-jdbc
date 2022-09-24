package com.example.realworldkotlinspringbootjdbc.domain.article

import arrow.core.Option
import arrow.core.ValidatedNel
import arrow.core.invalid
import arrow.core.invalidNel
import arrow.core.validNel
import arrow.core.zip
import arrow.typeclasses.Semigroup
import com.example.realworldkotlinspringbootjdbc.util.MyError

/**
 * フィード用パラメータ
 *
 * 特定のユーザーがお気に入りの人全員のそれぞれの最新記事一覧用のパラメータ郡
 *
 * limit: 1度に表示する記事の最大値
 * offset: 何個目から記事を表示するか(100個あるうち、offset=10で11個目から等)
 */
interface FeedParameters {
    val limit: Int
    val offset: Int

    /**
     * 実装
     */
    private data class ValidatedFeedParameters(
        override val limit: Int,
        override val offset: Int
    ) : FeedParameters

    /**
     * Factory メソッド
     */
    companion object {
        /**
         * new
         *
         * @param limit
         * @param offset
         * @return バリデーションエラー or フィード用パラメーター
         */
        fun new(
            limit: String? = null,
            offset: String? = null,
        ): ValidatedNel<ValidationError, FeedParameters> {
            val convertToValidatedLimit: (String?) -> ValidatedNel<ValidationError, Int> = { input ->
                Option.fromNullable(input).fold(
                    { ValidationError.LimitError.DEFAULT.validNel() },
                    { str ->
                        Option.fromNullable(str.toIntOrNull()).fold(
                            { ValidationError.LimitError.FailedConvertToInteger(str).invalidNel() },
                            { it.validNel() }
                        ).fold(
                            { it.invalid() },
                            {
                                when {
                                    it < ValidationError.LimitError.MINIMUM -> ValidationError.LimitError.RequireMinimumOrOver(it).invalidNel()
                                    it > ValidationError.LimitError.MAXIMUM -> ValidationError.LimitError.RequireMaximumOrUnder(it).invalidNel()
                                    else -> it.validNel()
                                }
                            }
                        )
                    }
                )
            }
            val convertToValidatedOffset: (String?) -> ValidatedNel<ValidationError, Int> = { input ->
                Option.fromNullable(input).fold(
                    { ValidationError.OffsetError.DEFAULT.validNel() },
                    { str ->
                        Option.fromNullable(str.toIntOrNull()).fold(
                            { ValidationError.OffsetError.FailedConvertToInteger(str).invalidNel() },
                            { it.validNel() }
                        ).fold(
                            { it.invalid() },
                            {
                                when {
                                    it < ValidationError.OffsetError.MINIMUM -> ValidationError.OffsetError.RequireMinimumOrOver(it).invalidNel()
                                    else -> it.validNel()
                                }
                            }
                        )
                    }
                )
            }

            /**
             * 引数 -> ValidatedNel<ValidationError, Int>
             */
            return convertToValidatedLimit(limit).zip(
                Semigroup.nonEmptyList(),
                convertToValidatedOffset(offset)
            ) { a, b ->
                ValidatedFeedParameters(
                    limit = a,
                    offset = b
                )
            }
        }
    }

    /**
     * ドメインルール
     */
    sealed interface ValidationError : MyError.ValidationError {
        sealed interface LimitError : ValidationError {
            companion object {
                const val DEFAULT = 20
                const val MINIMUM = 1
                const val MAXIMUM = 100
            }
            override val key: String get() = LimitError::class.simpleName.toString()
            data class FailedConvertToInteger(val value: String) : LimitError {
                override val message: String get() = "数値に変換できる数字にしてください"
            }
            data class RequireMinimumOrOver(val value: Int) : LimitError {
                override val message: String get() = "${MINIMUM}以上である必要があります"
            }
            data class RequireMaximumOrUnder(val value: Int) : LimitError {
                override val message: String get() = "${MAXIMUM}以下である必要があります"
            }
        }

        sealed interface OffsetError : ValidationError {
            companion object {
                const val DEFAULT = 0
                const val MINIMUM = 0
                // const val MAXIMUM = Int.MAX_VALUE
            }
            override val key: String get() = LimitError::class.simpleName.toString()
            data class FailedConvertToInteger(val value: String) : LimitError {
                override val message: String get() = "数値に変換できる数字にしてください"
            }
            data class RequireMinimumOrOver(val value: Int) : LimitError {
                override val message: String get() = "${MINIMUM}以上である必要があります"
            }
            /**
             * MAXIMUM = Int.MAX_VALUEなので実装しない
             */
        }
    }
}
