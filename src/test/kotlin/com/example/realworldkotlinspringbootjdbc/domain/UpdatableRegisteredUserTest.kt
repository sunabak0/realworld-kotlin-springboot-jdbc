package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.*
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UpdatableRegisteredUserTest {
    @Test
    fun `プロパティが全てNullの場合、バリデーションエラーが戻り値となる`() {
        val actual = UpdatableRegisteredUser.new(UserId(1), null, null, null, null)
        val expected = UpdatableRegisteredUser.ValidationError.AtLeastOneAttributeIsRequired.invalidNel()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `不正なプロパティが1つ以上ある場合、バリデーションエラーが戻り値となる`() {
        val invalidEmail = "invalid-email-format"
        val actual = UpdatableRegisteredUser.new(UserId(1), invalidEmail, null, null, null)
        val expected = Email.ValidationError.InvalidFormat(invalidEmail).invalidNel()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `正常と不正なプロパティが混ざっている場合、バリデーションエラーが戻り値となる`() {
        val invalidEmail = "invalid-email-format"
        val validUsername = "John"
        val actual = UpdatableRegisteredUser.new(UserId(1), invalidEmail, validUsername, null, null)
        val expected = Email.ValidationError.InvalidFormat(invalidEmail).invalidNel()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `更新したい正常なプロパティが1つ以上あり、残りがnullの場合、isValidはtrueをreturnする`() {
        val validUsername = "John"
        val actual = UpdatableRegisteredUser.new(UserId(1), null, validUsername, null, null)
        assertThat(actual.isValid).isTrue
    }
}
