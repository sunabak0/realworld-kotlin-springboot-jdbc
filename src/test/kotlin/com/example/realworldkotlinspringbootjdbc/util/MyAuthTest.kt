package com.example.realworldkotlinspringbootjdbc.util

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 *
 * authorize のテスト
 *
 * */
class MyAuthTest {
    @Test
    fun `引数のAuthorization Headerがnullの場合、Jwt認証は「Bearer Tokenを要求する」旨のエラーを返す`() {
        val notImplementedUserRepository = object : UserRepository {}
        val notImplementedMySessionJwt = object : MySessionJwt {}

        val actual = MyAuthImpl(notImplementedUserRepository, notImplementedMySessionJwt).authorize(null)
        val expected = MyAuth.Unauthorized.RequiredBearerToken.left()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Jwtデコード時に「失敗した」旨のエラーが返ってきた場合、Jwt認証は「Decodeに失敗した」旨のエラーを返す`() {
        val decodeError = MySessionJwt.DecodeError.NothingRequiredClaim("dummy")
        val notImplementedUserRepository = object : UserRepository {}
        val decodeReturnError = object : MySessionJwt {
            override fun decode(token: String): Either<MySessionJwt.DecodeError, MySession> =
                decodeError.left()
        }

        val actual = MyAuthImpl(notImplementedUserRepository, decodeReturnError).authorize("abc")
        val expected = MyAuth.Unauthorized.FailedDecodeToken(decodeError, "abc").left()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Jwtデコードは成功するが、UserRepositoryがユーザー検索時にエラーを返した場合、Jwt認証は「見つからなかった」旨のエラーを返す`() {
        val userId = UserId(1)
        val decodeReturnSuccess = object : MySessionJwt {
            override fun decode(token: String): Either<MySessionJwt.DecodeError, MySession> =
                MySession(
                    userId,
                    object : Email { override val value: String get() = "dummy@example.com" }
                ).right()
        }
        val notFoundError = UserRepository.FindByUserIdError.NotFound(userId)
        val findByUserIdReturnError = object : UserRepository {
            override fun findByUserId(id: UserId): Either<UserRepository.FindByUserIdError, RegisteredUser> =
                notFoundError.left()
        }

        val actual = MyAuthImpl(findByUserIdReturnError, decodeReturnSuccess).authorize("Bearer: dummy")
        val expected = MyAuth.Unauthorized.NotFound(notFoundError, userId).left()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `UserRepositoryがユーザー検索時にエラーを返した場合、Jwt認証は「見つからなかった」旨のエラーを返す`() {
        val userId = UserId(1)
        val decodeReturnSuccess = object : MySessionJwt {
            override fun decode(token: String): Either<MySessionJwt.DecodeError, MySession> =
                MySession(
                    userId,
                    object : Email { override val value: String get() = "dummy@example.com" }
                ).right()
        }
        val notFoundError = UserRepository.FindByUserIdError.NotFound(userId)
        val findByUserIdReturnError = object : UserRepository {
            override fun findByUserId(id: UserId): Either<UserRepository.FindByUserIdError, RegisteredUser> =
                notFoundError.left()
        }

        val actual = MyAuthImpl(findByUserIdReturnError, decodeReturnSuccess).authorize("Bearer: dummy")
        val expected = MyAuth.Unauthorized.NotFound(notFoundError, userId).left()
        assertThat(actual).isEqualTo(expected)
    }
}
