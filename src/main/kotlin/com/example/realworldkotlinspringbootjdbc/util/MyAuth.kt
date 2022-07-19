package com.example.realworldkotlinspringbootjdbc.util

import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.springframework.stereotype.Component

interface MyAuth {
    fun authorize(bearerToken: String?): Either<Unauthorized, RegisteredUser> = TODO()

    /**
     * エラーの種類(基本的に全て認証失敗)
     */
    interface Unauthorized : MyError {
        /**
         * Null は駄目
         */
        object RequiredBearerToken : Unauthorized, MyError.Basic
        /**
         * BearerTokenのパースに失敗
         */
        data class FailedParseBearerToken(override val cause: Throwable, val authorizationHeader: String) : Unauthorized, MyError.MyErrorWithThrowable
        /**
         * Decodeに失敗
         */
        data class FailedDecodeToken(override val cause: MyError, val token: String) : Unauthorized, MyError.MyErrorWithMyError
        /**
         * 検索したUserが存在しなかった
         */
        data class NotFound(override val cause: MyError, val id: UserId) : Unauthorized, MyError.MyErrorWithMyError
        /**
         * Emailが合わなかった
         */
        data class NotMatchEmail(val oldEmail: Email, val newEmail: Email) : Unauthorized, MyError.Basic
        /**
         * 謎エラー
         */
        data class Unexpected(override val cause: MyError, val bearerToken: Option<String>) : Unauthorized, MyError.MyErrorWithMyError
    }
}

@Component
class MyAuthImpl(
    val userRepository: UserRepository,
    val mySessionJwt: MySessionJwt,
) : MyAuth {
    /**
     * Bearer Tokenから認証
     */
    override fun authorize(authorizationHeader: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
        return Option.fromNullable(authorizationHeader)
            .toEither { MyAuth.Unauthorized.RequiredBearerToken }
            .flatMap { // Parse bearer header to token
                try { it.split(" ")[1].right() } catch (e: IndexOutOfBoundsException) {
                    MyAuth.Unauthorized.FailedParseBearerToken(e, it).left()
                }
            }.flatMap { // Decode token to MySession
                token ->
                mySessionJwt.decode(token).fold(
                    { MyAuth.Unauthorized.FailedDecodeToken(it, token).left() },
                    { it.right() }
                )
            }.flatMap { // Find RegisteredUser by MySession.UserId
                session ->
                userRepository.findByUserId(session.userId).fold(
                    {
                        when (it) {
                            is UserRepository.FindByUserIdError.NotFound -> MyAuth.Unauthorized.NotFound(it, session.userId).left()
                            is UserRepository.FindByUserIdError.Unexpected -> MyAuth.Unauthorized.Unexpected(it, Option.fromNullable(authorizationHeader)).left()
                        }
                    },
                    { Pair(session.email, it).right() }
                )
            }.flatMap { // Compare MySession.Email and RegisteredUser.Email
                val (sessionEmail, registeredUser) = it
                if (sessionEmail.value == registeredUser.email.value) { registeredUser.right() } else { println(sessionEmail.value); MyAuth.Unauthorized.NotMatchEmail(sessionEmail, registeredUser.email).left() }
            }
    }
}
