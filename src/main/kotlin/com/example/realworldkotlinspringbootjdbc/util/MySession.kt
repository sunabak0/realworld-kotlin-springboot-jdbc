package com.example.realworldkotlinspringbootjdbc.util

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import com.auth0.jwt.exceptions.JWTDecodeException
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.springframework.stereotype.Component

data class MySession(
    val userId: UserId,
    val email: Email
)

interface MySessionJwt {
    companion object {
        const val ISSUER = "RealWorld"
        const val USER_ID_KEY = "userId"
        const val EMAIL_KEY = "email"
    }

    fun decode(token: String): Either<DecodeError, MySession> = throw NotImplementedError()
    fun encode(session: MySession): Either<EncodeError, String> = throw NotImplementedError()

    sealed interface DecodeError : MyError {
        data class FailedDecode(override val cause: JWTDecodeException, val token: String) :
            DecodeError,
            MyError.MyErrorWithThrowable

        data class NotMatchIssuer(val token: String, val actual: String, val expected: String) :
            DecodeError,
            MyError.Basic

        data class NothingRequiredClaim(val token: String) : DecodeError, MyError
    }

    sealed interface EncodeError : MyError {
        data class FailedEncode(override val cause: JWTCreationException, val session: MySession) :
            EncodeError,
            MyError.MyErrorWithThrowable
    }
}

@Component
object MySessionJwtImpl : MySessionJwt {
    override fun decode(token: String): Either<MySessionJwt.DecodeError, MySession> {
        val decodedToken = try {
            JWT.decode(token)
        } catch (e: JWTDecodeException) {
            return MySessionJwt.DecodeError.FailedDecode(cause = e, token).left()
        }
        return try {
            val userId = decodedToken.getClaim(MySessionJwt.USER_ID_KEY).asInt()
            val email = decodedToken.getClaim(MySessionJwt.EMAIL_KEY).asString()
            if (decodedToken.issuer == MySessionJwt.ISSUER) {
                val session = MySession(
                    UserId(userId),
                    Email.newWithoutValidation(email)
                )
                session.right()
            } else {
                MySessionJwt.DecodeError.NotMatchIssuer(token, decodedToken.issuer, MySessionJwt.ISSUER).left()
            }
        } catch (e: NullPointerException) {
            MySessionJwt.DecodeError.NothingRequiredClaim(token).left()
        }
    }

    override fun encode(session: MySession): Either<MySessionJwt.EncodeError, String> {
        val secret = "secret"
        return try {
            val token = JWT.create()
                .withIssuer(MySessionJwt.ISSUER)
                .withClaim(MySessionJwt.USER_ID_KEY, session.userId.value)
                .withClaim(MySessionJwt.EMAIL_KEY, session.email.value)
                .sign(Algorithm.HMAC256(secret))
            token.right()
        } catch (e: JWTCreationException) {
            MySessionJwt.EncodeError.FailedEncode(e, session).left()
        }
    }
}
