package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.*
import arrow.typeclasses.Semigroup
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface UpdatableRegisteredUser {
    val userId: UserId
    val email: Option<Email>
    val username: Option<Username>
    val bio: Option<Bio>
    val image: Option<Image>

    /**
     * 実装
     */
    private data class ValidatedUpdatableRegisteredUser(
        override val userId: UserId,
        override val email: Option<Email>,
        override val username: Option<Username>,
        override val bio: Option<Bio>,
        override val image: Option<Image>,
    ) : UpdatableRegisteredUser

    /**
     * Factory メソッド
     */
    companion object {
        /**
         * Validation 有り
         */
        fun new(
            userId: UserId,
            email: String?,
            username: String?,
            bio: String?,
            image: String?,
        ): ValidatedNel<MyError.ValidationError, UpdatableRegisteredUser> {
            // TODO: Genericsを使ってリファクタできそう(<T>を利用して処理の共通化できそう)
            // Email(null許容)
            val newEmailAllowedNull: () -> ValidatedNel<Email.ValidationError, Option<Email>> = { ->
                Option.fromNullable(email).fold(
                    { None.validNel() },
                    { Email.new(it).map { validated -> Some(validated) } }
                )
            }
            // Username(null許容)
            val newUsernameAllowedNull: () -> ValidatedNel<Username.ValidationError, Option<Username>> = { ->
                Option.fromNullable(username).fold(
                    { None.validNel() },
                    { Username.new(it).map { validated -> Some(validated) } }
                )
            }
            // Bio(null許容)
            val newBioAllowedNull: () -> ValidatedNel<Bio.ValidationError, Option<Bio>> = { ->
                Option.fromNullable(bio).fold(
                    { None.validNel() },
                    { Bio.new(it).map { validated -> Some(validated) } }
                )
            }
            // Image(null許容)
            val newImageAllowedNull: () -> ValidatedNel<Image.ValidationError, Option<Image>> = { ->
                Option.fromNullable(image).fold(
                    { None.validNel() },
                    { Image.new(it).map { validated -> Some(validated) } }
                )
            }

            /**
             * プロパティは1つ1つのnullは許容している(=正常扱い)
             * しかし、全てnullの場合のみ: 異常とする
             *
             * Null以外のバリデーションエラーが1つでもある場合: OUT
             */
            return newEmailAllowedNull().zip(
                Semigroup.nonEmptyList(),
                newUsernameAllowedNull(),
                newBioAllowedNull(),
                newImageAllowedNull()
            ) { a, b, c, d -> Tuple4(a, b, c, d) }.fold(
                { it.invalid() }, // 1つでもバリデーションエラーがある場合: OUT
                {
                    val (a, b, c, d) = it
                    ValidationError.AtLeastOneAttributeIsRequired.check(a, b, c, d).fold(
                        { validationError -> validationError.invalidNel() },
                        { ValidatedUpdatableRegisteredUser(userId, a, b, c, d).validNel() }
                    )
                }
            )
        }
    }

    /**
     * ドメインルール
     */
    sealed interface ValidationError : MyError.ValidationError {
        /**
         * 全て Noneは駄目
         */
        object AtLeastOneAttributeIsRequired : ValidationError {
            override val key: String get() = UpdatableRegisteredUser::class.simpleName.toString()
            override val message: String get() = "更新するプロパティを1つ以上指定してください"
            fun check(a: Option<Email>, b: Option<Username>, c: Option<Bio>, d: Option<Image>): Validated<AtLeastOneAttributeIsRequired, Unit> =
                when (Tuple4(a, b, c, d)) {
                    Tuple4(None, None, None, None) -> { AtLeastOneAttributeIsRequired.invalid() }
                    else -> { Unit.valid() }
                }
        }
    }
}
