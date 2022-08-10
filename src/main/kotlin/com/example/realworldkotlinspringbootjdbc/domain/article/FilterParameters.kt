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
 * フィルタ用パラメーター
 *
 * 作成済み記事一覧のフィルタに使うパラメータ郡
 *
 * tag: タグでフィルタ
 * author: 書いた人でフィルタ
 * favoritesByUsername: 特定のユーザーがお気に入り済みかどうかでフィルタ
 * limit: 1度に表示する記事の最大値
 * offset: 何個目から記事を表示するか(100個あるうち、offset=10で11個目から等)
 *
 * 注
 * - Option<T>の時、Noneはフィルタなしを表現する
 * - Usernameなどを利用して、バリデーションは通さない
 *   - 例: InvalidなUsernameをNoneにした時、フィルタ無しになってしまうため
 */
interface FilterParameters {
    val tag: Option<String>
    val author: Option<String>
    val favoritedByUsername: Option<String>
    val limit: Int
    val offset: Int

    /**
     * 実装
     */
    private data class ValidatedFilterParameters(
        override val tag: Option<String>,
        override val author: Option<String>,
        override val favoritedByUsername: Option<String>,
        override val limit: Int,
        override val offset: Int
    ) : FilterParameters

    /**
     * Factory メソッド
     */
    companion object {
        /**
         * new
         *
         * @param tag
         * @param author
         * @param favoritedByUsername
         * @param limit
         * @param offset
         * @return バリデーションエラー or フィルタ用パラメーター
         */
        fun new(
            tag: String?,
            author: String?,
            favoritedByUsername: String?,
            limit: String?,
            offset: String?,
        ): ValidatedNel<ValidationError, FilterParameters> {
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
                ValidatedFilterParameters(
                    tag = Option.fromNullable(tag),
                    author = Option.fromNullable(author),
                    favoritedByUsername = Option.fromNullable(favoritedByUsername),
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
                // private const val MAXIMUM = Int.MAX_VALUE
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
