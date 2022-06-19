package com.example.realworldkotlinspringbootjdbc.util

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MyAuthTest {
    @Test
    fun `引数のAuthorization Headerがnullの場合、セッションからの認証は「Bearer Tokenを要求する」旨のエラーを返す`() {
        val dummyUserRepository = object : UserRepository {}
        val dummyMySessionJwt = object : MySessionJwt {}
        val actual = MyAuthImpl(dummyUserRepository, dummyMySessionJwt).authorize(null)
        val expected = MyAuth.Unauthorized.RequiredBearerToken.left()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `引数のAuthorization HeaderがBearer tokenとしての形式が異なった場合、セッションからの認証は「Parseに失敗した」旨のエラーを返す`() {
        val dummyUserRepository = object : UserRepository {}
        val dummyMySessionJwt = object : MySessionJwt {}
        val authorizationHeader = "dummy-empty"
        // test
        // TODO: ThrowableのCompare方法
        when (val actual = MyAuthImpl(dummyUserRepository, dummyMySessionJwt).authorize(authorizationHeader)) {
            is Left -> when (val error = actual.value) {
                is MyAuth.Unauthorized.FailedParseBearerToken -> assertThat(error.authorizationHeader).isEqualTo(authorizationHeader)
                else -> assert(false)
            }
            is Right -> assert(false)
        }
    }
}
