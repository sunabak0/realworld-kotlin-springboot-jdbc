package com.example.realworldkotlinspringbootjdbc.domain.article.filter_parameters

import arrow.core.ValidatedNel
import arrow.core.invalidNel
import arrow.core.valid
import arrow.core.zip
import arrow.typeclasses.Semigroup
import com.example.realworldkotlinspringbootjdbc.util.MyError

/**
 * リミット
 *
 * 作成済み記事の一覧を表示する時、1度に表示するの最大量
 */
interface Limit {
    val value: Int

    /**
     * 実装
     */
    private data class ValidatedLimit(override val value: Int) : Limit

    /**
     * Factory メソッド
     */
    companion object {
        private const val DEFAULT_VALUE = 20

        /**
         * new
         *
         * - Validation 有り
         *
         * @param limit リミットを数字で表現した値(null: 可)
         * @return バリデーションエラー or リミット(引数limitがnullの場合はデフォルト値)
         */
        fun new(limit: String?): ValidatedNel<ValidationError, Limit> {
            val limitOrDefaultString = limit ?: DEFAULT_VALUE.toString()
            val convertedLimit = try {
                limitOrDefaultString.toInt()
            } catch (e: NumberFormatException) {
                return ValidationError.FailedConvertToInteger(limitOrDefaultString).invalidNel()
            }
            return ValidationError.RequireMinimumOrOver.check(convertedLimit).zip(
                Semigroup.nonEmptyList(),
                ValidationError.RequireMaximumOrUnder.check(convertedLimit)
            ) { _, _ -> ValidatedLimit(convertedLimit) }
        }
    }

    /**
     * ドメインルール
     */
    sealed interface ValidationError : MyError.ValidationError {
        override val key: String get() = Limit::class.simpleName.toString()
        /**
         * Intへ変換できる必要がある
         */
        data class FailedConvertToInteger(val value: String) : ValidationError {
            override val message: String get() = "数値に変換できる数字にしてください"
        }
        /**
         * 1 以上
         */
        data class RequireMinimumOrOver(val value: Int) : ValidationError {
            companion object {
                private const val minimum: Int = 1
                fun check(limit: Int): ValidatedNel<ValidationError, Unit> =
                    if (minimum <= limit) {
                        Unit.valid()
                    } else {
                        RequireMinimumOrOver(limit).invalidNel()
                    }
            }
            override val message: String get() = "${minimum}以上である必要があります"
        }
        /**
         * 100 以下
         */
        data class RequireMaximumOrUnder(val value: Int) : ValidationError {
            companion object {
                private const val maximum: Int = 100
                fun check(limit: Int): ValidatedNel<ValidationError, Unit> =
                    if (limit <= maximum) {
                        Unit.valid()
                    } else {
                        RequireMaximumOrUnder(limit).invalidNel()
                    }
            }
            override val message: String get() = "${maximum}以下である必要があります"
        }
    }
}
