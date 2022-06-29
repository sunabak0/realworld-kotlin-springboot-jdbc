package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.Option
import arrow.core.Validated
import arrow.core.ValidatedNel
import arrow.core.invalid
import arrow.core.invalidNel
import arrow.core.valid
import arrow.core.zip
import arrow.typeclasses.Semigroup
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface Password {
    val value: String

    /**
     * 実装
     */
    private data class PasswordImpl(override val value: String) : Password

    /**
     * Factory メソッド
     */
    companion object {
        /**
         * Validation 無し
         */
        fun newWithoutValidation(password: String): Password = PasswordImpl(password)

        /**
         * Validation 有り
         */
        fun new(password: String?): ValidatedNel<ValidationError, Password> =
            when (val result = ValidationError.Required.check(password)) {
                is Validated.Invalid -> result.value.invalidNel()
                is Validated.Valid -> {
                    val existedPassword = result.value
                    ValidationError.TooShort.check(existedPassword).zip(
                        Semigroup.nonEmptyList(),
                        ValidationError.TooLong.check(existedPassword)
                    ) { _, _ -> PasswordImpl(existedPassword) }
                }
            }

        /**
         * Login用 パスワード
         */
        fun newForLogin(password: String?): ValidatedNel<ValidationError, Password> =
            when (val result = ValidationError.Required.check(password)) {
                is Validated.Invalid -> result.value.invalidNel()
                is Validated.Valid -> PasswordImpl(result.value).valid()
            }
    }

    /**
     * ドメインルール
     */
    sealed interface ValidationError : MyError.ValidationError {
        override val key: String get() = Password::class.simpleName.toString()
        /**
         * Nullは駄目
         */
        object Required : ValidationError {
            override val message: String get() = "パスワードを入力してください。"
            fun check(password: String?): Validated<Required, String> =
                Option.fromNullable(password).fold(
                    { Required.invalid() },
                    { it.valid() }
                )
        }

        /**
         * 短すぎては駄目
         */
        data class TooShort(val password: String) : ValidationError {
            companion object {
                private const val minimum: Int = 8
                fun check(password: String): ValidatedNel<ValidationError, Unit> =
                    if (minimum <= password.length) { Unit.valid() } else { TooShort(password).invalidNel() }
            }
            override val message: String get() = "パスワードは${minimum}文字以上にしてください。"
        }

        /**
         * 長すぎては駄目
         */
        data class TooLong(val password: String) : ValidationError {
            companion object {
                private const val maximum: Int = 32
                fun check(password: String): ValidatedNel<ValidationError, Unit> =
                    if (password.length <= maximum) { Unit.valid() } else { TooLong(password).invalidNel() }
            }
            override val message: String get() = "パスワードは${maximum}文字以下にしてください。"
        }
    }
}
