package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.ValidatedNel
import arrow.core.zip
import arrow.typeclasses.Semigroup
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Password
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError.ValidationError

interface UnregisteredUser {
    val email: Email
    val password: Password
    val username: Username

    /**
     * 実装
     */
    private data class ValidatedUnregisteredUser(
        override val email: Email,
        override val password: Password,
        override val username: Username,
    ) : UnregisteredUser

    /**
     * Factory メソッド
     */
    companion object {
        /**
         * Validation 有り
         */
        fun new(
            email: String?,
            password: String?,
            username: String?,
        ): ValidatedNel<ValidationError, UnregisteredUser> =
            Email.new(email).zip(
                Semigroup.nonEmptyList(),
                Password.new(password),
                Username.new(username)
            ) { a, b, c -> ValidatedUnregisteredUser(a, b, c) }
    }
}
