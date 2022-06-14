package com.example.realworldkotlinspringbootjdbc.sandbox.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

//
// auth0/java-jwt
//
// の使い方
//
class Javajwt {
    @Test
    fun `【GitHubのQuickstart】 Create and Sign a Token`() {
        val encodedToken = try {
            JWT.create()
                .withIssuer("sandbox")
                .sign(Algorithm.HMAC256("secret"))
        } catch (e: JWTCreationException) {
            // Invalid Signing configuration / Couldn't convert Claims.
            throw e
        }
        val expected = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhdXRoMCJ9.izVguZPRsBQ5Rqw6dhMvcIwy8_9lQnrO3vpxGwPCuzs"
        assertThat(encodedToken).isEqualTo(expected)
    }

    @Test
    fun `【GitHubのQuickstart】 Verify a Token`() {
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXUyJ9.eyJpc3MiOiJhdXRoMCJ9.AbIJTDMFc7yUa5MhvcP03nJPyCPzZtQcGEp-zWfOkEE"
        val algorithm = Algorithm.HMAC256("secret") // use more secure key
        val verifier = JWT.require(algorithm)
            .withIssuer("sandbox")
            .build(); // Reusable verifier instance
        val jwt = try {
            verifier.verify(token)
        } catch (e: JWTVerificationException) {
            // Invalid signature/claims
            throw e
        }
        assertThat(jwt.issuer).isEqualTo("sandbox")
    }

    @Test
    fun `【GitHubのQuickstart】 Decode a Token`() {
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXUyJ9.eyJpc3MiOiJhdXRoMCJ9.AbIJTDMFc7yUa5MhvcP03nJPyCPzZtQcGEp-zWfOkEE"
        val decodedToken: DecodedJWT = try {
            JWT.decode(token)
        } catch (e: JWTDecodeException) {
            // Invalid token
            throw e
        }
        assertThat(decodedToken.issuer).isEqualTo("sandbox")
    }

    @Test
    fun `好きなClaimを入れてEncodeしてDecodeして取得`() {
        val id = 123
        val email = "dummy@example.com"
        val secret = "secret"
        val encodedToken = try {
            JWT.create()
                .withIssuer("sandbox")
                .withClaim("id", id)
                .withClaim("email", email)
                .sign(Algorithm.HMAC256(secret))
        } catch (e: JWTCreationException) {
            // Invalid Signing configuration / Couldn't convert Claims.
            throw e
        }
        val decodedToken = try {
            JWT.decode(encodedToken)
        } catch (e: JWTDecodeException) {
            // Invalid token
            throw e
        }
        assertThat(decodedToken.getClaim("id").asInt()).isEqualTo(id)
        assertThat(decodedToken.getClaim("email").asString()).isEqualTo(email)
    }

    @Test
    fun `存在しないClaimを取得しようとした時の挙動`() {
        val secret = "secret"
        val encodedToken = try {
            JWT.create()
                .withIssuer("sandbox")
                .sign(Algorithm.HMAC256(secret))
        } catch (e: JWTCreationException) {
            // Invalid Signing configuration / Couldn't convert Claims.
            throw e
        }
        val decodedToken = try {
            JWT.decode(encodedToken)
        } catch (e: JWTDecodeException) {
            // Invalid token
            throw e
        }
        assertThat(decodedToken.getClaim("foobar").isNull).isTrue
    }

    @Test
    fun `存在するがCastに失敗した時はNullPointerException`() {
        val secret = "secret"
        val email = "dummy@example.com"
        val encodedToken = try {
            JWT.create()
                .withIssuer("sandbox")
                .withClaim("email", email)
                .sign(Algorithm.HMAC256(secret))
        } catch (e: JWTCreationException) {
            // Invalid Signing configuration / Couldn't convert Claims.
            throw e
        }
        val decodedToken = try {
            JWT.decode(encodedToken)
        } catch (e: JWTDecodeException) {
            // Invalid token
            throw e
        }
        // println(decodedToken.getClaim("email").asInt())
    }
}
