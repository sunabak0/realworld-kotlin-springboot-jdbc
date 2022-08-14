package com.example.realworldkotlinspringbootjdbc.util

import arrow.core.left
import arrow.core.right
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MySessionJwtTest {
    class EncodeTest {
        @Test
        fun `正常系-基本的に成功する`() {
            /**
             * given:
             */
            val mySession = MySession(
                userId = UserId(1),
                email = Email.newWithoutValidation("kotlin@example.com")
            )

            /**
             * when:
             */
            val actual = MySessionJwtImpl.encode(mySession)

            /**
             * then:
             */
            val expected = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJSZWFsV29ybGQiLCJ1c2VySWQiOjEsImVtYWlsIjoia290bGluQGV4YW1wbGUuY29tIn0.1c15fHlulol504uMdSbOdbyF7bygSNhtIEofMy4KyU4".right()
            assertThat(actual).isEqualTo(expected)
        }
    }

    class DecodeTest {
        @Test
        fun `正常系-成功した場合、MySessionが戻り値となる`() {
            /**
             * given:
             */
            val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJSZWFsV29ybGQiLCJ1c2VySWQiOjEsImVtYWlsIjoia290bGluQGV4YW1wbGUuY29tIn0.1c15fHlulol504uMdSbOdbyF7bygSNhtIEofMy4KyU4"

            /**
             * when:
             */
            val actual = MySessionJwtImpl.decode(token)

            /**
             * then:
             */
            val expected = MySession(
                userId = UserId(1),
                email = Email.newWithoutValidation("kotlin@example.com")
            ).right()
            assertThat(actual).isEqualTo(expected)
        }

        /**
         * 基本的に起きない想定
         */
        @Test
        fun `準正常系-別のIssuerでencodeしたjwtをdecodeした場合、その旨がわかるエラーが戻り値となる`() {
            /**
             * given:
             * - Issuerが異なる
             */
            val anotherIssuer = "another-issuer"
            val token = JWT.create()
                .withIssuer(anotherIssuer)
                .withClaim(MySessionJwt.USER_ID_KEY, 1)
                .withClaim(MySessionJwt.EMAIL_KEY, "kotlin@exampl.com")
                .sign(Algorithm.HMAC256("secret"))

            /**
             * when:
             */
            val actual = MySessionJwtImpl.decode(token)

            /**
             * then:
             */
            val expected = MySessionJwt.DecodeError.NotMatchIssuer(
                token = token,
                actual = anotherIssuer,
                expected = MySessionJwt.ISSUER
            ).left()
            assertThat(actual).isEqualTo(expected)
        }

        /**
         * 基本的に起きない想定
         */
        @Test
        fun `準正常系-Claimがなかった場合、その旨がわかるエラーが戻り値となる`() {
            /**
             * given:
             * - ClaimにuserIdが無い
             */
            val token = JWT.create()
                .withIssuer(MySessionJwt.ISSUER)
                // .withClaim(MySessionJwt.USER_ID_KEY, 123)
                .withClaim(MySessionJwt.EMAIL_KEY, "kotlin@exampl.com")
                .sign(Algorithm.HMAC256("secret"))

            /**
             * when:
             */
            val actual = MySessionJwtImpl.decode(token)

            /**
             * then:
             */
            val expected = MySessionJwt.DecodeError.NothingRequiredClaim(
                token = token
            ).left()
            assertThat(actual).isEqualTo(expected)
        }

        /**
         * 基本的に起きない想定
         */
        @Test
        fun `準正常系-Claimの型変換に失敗する場合、「Claimが無い」旨のエラーが戻り値となる`() {
            /**
             * given:
             * - userIdがIntではなく、String
             */
            val userId = "userid-is-string"
            val token = JWT.create()
                .withIssuer(MySessionJwt.ISSUER)
                .withClaim(MySessionJwt.USER_ID_KEY, userId)
                .withClaim(MySessionJwt.EMAIL_KEY, "kotlin@exampl.com")
                .sign(Algorithm.HMAC256("secret"))

            /**
             * when:
             */
            val actual = MySessionJwtImpl.decode(token)

            /**
             * then:
             */
            val expected = MySessionJwt.DecodeError.NothingRequiredClaim(
                token = token
            ).left()
            assertThat(actual).isEqualTo(expected)
        }
    }
}
