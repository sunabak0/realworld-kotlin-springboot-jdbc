package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.Option
import arrow.core.Validated
import arrow.core.ValidatedNel
import arrow.core.invalid
import arrow.core.invalidNel
import arrow.core.valid
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface Email {
    val value: String

    /**
     * 実装
     */
    private data class ValidatedEmail(override val value: String) : Email
    private data class EmailWithoutValidation(override val value: String) : Email

    /**
     * Factory メソッド
     */
    companion object {
        /**
         * Validation 無し
         */
        fun newWithoutValidation(email: String): Email = EmailWithoutValidation(email)

        /**
         * Validation 有り
         */
        fun new(email: String?): ValidatedNel<ValidationError, Email> =
            when (val result = ValidationError.Required.check(email)) {
                is Validated.Invalid -> result.value.invalidNel()
                is Validated.Valid -> {
                    val existedEmail = result.value
                    ValidationError.InvalidFormat.check(existedEmail)
                        .map { ValidatedEmail(existedEmail) }
                }
            }
    }

    /**
     * ドメインルール
     */
    sealed interface ValidationError : MyError.ValidationError {
        override val key: String get() = Email::class.simpleName.toString()

        /**
         * Nullは駄目
         */
        object Required : ValidationError {
            override val message: String get() = "メールアドレスを入力してください。"
            fun check(password: String?): Validated<Required, String> =
                Option.fromNullable(password).fold(
                    { Required.invalid() },
                    { it.valid() }
                )
        }

        /**
         * メールアドレスの形式であること
         *
         * 参考
         * https://android.googlesource.com/platform/frameworks/base/+/81aa097/core/java/android/util/Patterns.java#146
         */
        data class InvalidFormat(val email: String) : ValidationError {
            override val message: String get() = "メールアドレスが不正な形式です。(正しい形式例：john@example.com)"
            companion object {
                private const val emailPattern = """[a-zA-Z0-9+._%\-+]{1,256}""" +
                    """\@""" +
                    """[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}""" +
                    "(" +
                    """\.""" +
                    """[a-zA-Z0-9][a-zA-Z0-9\-]{0,25}""" +
                    ")+"
                fun check(email: String): ValidatedNel<ValidationError, Unit> =
                    if (email.matches(emailPattern.toRegex())) { Unit.valid() } else { InvalidFormat(email).invalidNel() }
            }
        }
    }
}
