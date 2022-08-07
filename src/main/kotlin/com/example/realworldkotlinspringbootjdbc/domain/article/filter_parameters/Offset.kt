package com.example.realworldkotlinspringbootjdbc.domain.article.filter_parameters

import arrow.core.ValidatedNel
import arrow.core.invalidNel
import arrow.core.valid
import com.example.realworldkotlinspringbootjdbc.util.MyError

/**
 * オフセット
 *
 * 作成済み記事の一覧を表示する時、スキップする数
 * (例: オフセットが10の時、記事は11個目から表示される)
 */
interface Offset {
    val value: Int

    /**
     * 実装
     */
    private data class ValidatedOffset(override val value: Int) : Offset

    /**
     * Factory メソッド
     */
    companion object {
        private const val DEFAULT_VALUE = 0

        /**
         * new
         *
         * - バリデーション有り
         *
         * @param offset オフセットを数字で表現した値(null: 可)
         * @return バリデーションエラー or オフセット(引数offsetがnullの場合はデフォルト値)
         */
        fun new(offset: String?): ValidatedNel<MyError.ValidationError, Offset> {
            val offsetOrDefaultString = offset ?: DEFAULT_VALUE.toString()
            val convertedOffset = try {
                offsetOrDefaultString.toInt()
            } catch (e: NumberFormatException) {
                return ValidationError.FailedConvertToInteger(offsetOrDefaultString).invalidNel()
            }
            return ValidationError.RequireMinimumOrOver.check(convertedOffset)
                .map { ValidatedOffset(convertedOffset) }
        }
    }

    /**
     * ドメインルール
     */
    sealed interface ValidationError : MyError.ValidationError {
        override val key: String get() = Offset::class.simpleName.toString()
        /**
         * Intへ変換できる必要がある
         */
        data class FailedConvertToInteger(val value: String) : ValidationError {
            override val message: String get() = "数値に変換できる数字にしてください"
        }
        /**
         * 0 以上
         */
        data class RequireMinimumOrOver(val value: Int) : ValidationError {
            companion object {
                private const val minimum: Int = 0
                fun check(offset: Int): ValidatedNel<ValidationError, Unit> =
                    if (minimum <= offset) {
                        Unit.valid()
                    } else {
                        RequireMinimumOrOver(offset).invalidNel()
                    }
            }
            override val message: String get() = "${minimum}以上である必要があります"
        }
        /**
         * Int.MAX_VALUE 以下
         * (Intに変換できた時点でOKなので) 実装はナシ
         */
    }
}
