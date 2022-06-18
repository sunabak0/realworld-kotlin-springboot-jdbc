package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.*
import arrow.typeclasses.Semigroup
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface Username {
    val value: String

    //
    // 実装
    //
    private data class ValidatedUsername(override val value: String) : Username
    private data class UsernameWithoutValidation(override val value: String) : Username

    //
    // Factory メソッド
    //
    companion object {
        //
        // Validation無し
        //
        fun newWithoutValidation(username: String): Username = UsernameWithoutValidation(username)

        //
        // Validation有り
        //
        fun new(username: String?): ValidatedNel<ValidationError, Username> {
            return when (val result = ValidationError.Required.check(username)) {
                is Validated.Invalid -> { return result.value.invalidNel() }
                is Validated.Valid -> {
                    val existedUsername = result.value
                    ValidationError.TooShort.check(existedUsername).zip(
                        Semigroup.nonEmptyList(),
                        ValidationError.TooLong.check(existedUsername)
                    ) { _, _ -> ValidatedUsername(existedUsername) }
                        .handleErrorWith { it.invalid() }
                }
            }
        }
    }

    //
    // ドメインルール
    //
    sealed interface ValidationError : MyError.ValidationError {
        override val key: String get() = Username::class.simpleName.toString()
        //
        // Nullは駄目
        //
        object Required : ValidationError {
            override val message: String get() = "ユーザ名を入力してください。"
            fun check(username: String?): Validated<Required, String> =
                Option.fromNullable(username).fold(
                    { Validated.Invalid(Required) },
                    { Validated.Valid(it) }
                )
        }

        //
        // 短すぎては駄目
        //
        data class TooShort(val username: String) : ValidationError {
            companion object {
                private const val minimum: Int = 4
                fun check(username: String): ValidatedNel<ValidationError, Unit> =
                    if (minimum <= username.length) { Unit.validNel() }
                    else { TooShort(username).invalidNel() }
            }
            override val message: String get() = "ユーザー名は${minimum}文字以上にしてください。"
        }

        //
        // 長すぎては駄目
        //
        data class TooLong(val username: String) : ValidationError {
            companion object {
                private const val maximum: Int = 32
                fun check(username: String): ValidatedNel<ValidationError, Unit> =
                    if (username.length <= maximum) { Unit.validNel() }
                    else { TooLong(username).invalidNel() }
            }
            override val message: String get() = "ユーザー名は${maximum}文字以下にしてください。"
        }
    }
}