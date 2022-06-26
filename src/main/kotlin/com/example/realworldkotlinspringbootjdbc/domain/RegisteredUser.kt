package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.ValidatedNel
import arrow.core.zip
import arrow.typeclasses.Semigroup
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface RegisteredUser {
    val userId: UserId
    val email: Email
    val username: Username
    val bio: Bio
    val image: Image

    //
    // 実装
    //
    private data class ValidatedRegisteredUser(
        override val userId: UserId,
        override val email: Email,
        override val username: Username,
        override val bio: Bio,
        override val image: Image,
    ) : RegisteredUser
    private data class WithoutValidationRegisteredUser(
        override val userId: UserId,
        override val email: Email,
        override val username: Username,
        override val bio: Bio,
        override val image: Image,
    ) : RegisteredUser

    //
    // Factory メソッド
    //
    companion object {
        //
        // Validation 無し
        //
        fun newWithoutValidation(
            userId: UserId,
            email: Email,
            username: Username,
            bio: Bio,
            image: Image
        ): RegisteredUser = WithoutValidationRegisteredUser(
            userId,
            email,
            username,
            bio,
            image,
        )

        //
        // Validation 有り
        //
        fun new(
            userId: Int,
            email: String?,
            username: String?,
            bio: String?,
            image: String?
        ): ValidatedNel<MyError.ValidationError, RegisteredUser> =
            Email.new(email).zip(
                Semigroup.nonEmptyList(),
                Username.new(username),
                Bio.new(bio),
                Image.new(image)
            ) { a, b, c, d -> ValidatedRegisteredUser(UserId(userId), a, b, c, d) }
    }
}
