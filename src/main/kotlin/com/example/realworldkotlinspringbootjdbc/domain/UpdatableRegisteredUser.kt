package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.None
import arrow.core.Option
import arrow.core.Tuple4
import arrow.core.Validated
import arrow.core.ValidatedNel
import arrow.core.invalid
import arrow.core.invalidNel
import arrow.core.valid
import arrow.core.validNel
import arrow.core.zip
import arrow.typeclasses.Semigroup
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError
import java.util.Date

interface UpdatableRegisteredUser {
    val userId: UserId
    val email: Email
    val username: Username
    val bio: Bio
    val image: Image
    val updatedAt: Date

    /**
     * 実装
     */
    private data class ValidatedUpdatableRegisteredUser(
        override val userId: UserId,
        override val email: Email,
        override val username: Username,
        override val bio: Bio,
        override val image: Image,
        override val updatedAt: Date = Date(),
    ) : UpdatableRegisteredUser

    /**
     * Factory メソッド
     */
    companion object {
        /**
         * Validation 有り
         */
        fun new(
            currentUser: RegisteredUser,
            email: String?,
            username: String?,
            bio: String?,
            image: String?,
        ): ValidatedNel<MyError.ValidationError, UpdatableRegisteredUser> {
            // TODO: Genericsを使ってリファクタできそう(<T>を利用して処理の共通化できそう)
            // Email
            val newEmailAllowedNull: () -> ValidatedNel<Email.ValidationError, Email> = { ->
                Option.fromNullable(email).fold(
                    { currentUser.email.validNel() }, // nullの場合は、currentUserの属性をそのまま利用する
                    { Email.new(it) }
                )
            }
            // Username
            val newUsernameAllowedNull: () -> ValidatedNel<Username.ValidationError, Username> = { ->
                Option.fromNullable(username).fold(
                    { currentUser.username.validNel() }, // nullの場合は、currentUserの属性をそのまま利用する
                    { Username.new(it) }
                )
            }
            // Bio
            val newBioAllowedNull: () -> ValidatedNel<Bio.ValidationError, Bio> = { ->
                Option.fromNullable(bio).fold(
                    { currentUser.bio.validNel() }, // nullの場合は、currentUserの属性をそのまま利用する
                    { Bio.new(it) }
                )
            }
            // Image
            val newImageAllowedNull: () -> ValidatedNel<Image.ValidationError, Image> = { ->
                Option.fromNullable(image).fold(
                    { currentUser.image.validNel() }, // nullの場合は、currentUserの属性をそのまま利用する
                    { Image.new(it) }
                )
            }

            @Suppress("DestructuringDeclarationWithTooManyEntries")
            return newEmailAllowedNull().zip(
                Semigroup.nonEmptyList(),
                newUsernameAllowedNull(),
                newBioAllowedNull(),
                newImageAllowedNull()
            ) { a, b, c, d -> Tuple4(a, b, c, d) }.fold(
                { it.invalid() }, // 1つでもバリデーションエラーがある場合: Invalid
                {
                    if (it == Tuple4(currentUser.email, currentUser.username, currentUser.bio, currentUser.image)) {
                        ValidationError.NothingAttributeToUpdatable.invalidNel() // 新旧が同じ場合: Invalid
                    } else {
                        val (a, b, c, d) = it
                        ValidatedUpdatableRegisteredUser(currentUser.userId, a, b, c, d).validNel()
                    }
                }
            )
        }
    }

    /**
     * ドメインルール
     */
    sealed interface ValidationError : MyError.ValidationError {
        /**
         * 変更が1つもないのは駄目
         */
        object NothingAttributeToUpdatable : ValidationError {
            override val key: String get() = UpdatableRegisteredUser::class.simpleName.toString()
            override val message: String get() = "更新するプロパティが有りません"
            fun check(
                a: Option<Email>,
                b: Option<Username>,
                c: Option<Bio>,
                d: Option<Image>
            ): Validated<NothingAttributeToUpdatable, Unit> =
                when (Tuple4(a, b, c, d)) {
                    Tuple4(None, None, None, None) -> { NothingAttributeToUpdatable.invalid() }
                    else -> { Unit.valid() }
                }
        }
    }
}
