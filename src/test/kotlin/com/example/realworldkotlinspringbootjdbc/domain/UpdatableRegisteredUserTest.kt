package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.invalidNel
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UpdatableRegisteredUserTest {
    private val validatedRegisteredUser = RegisteredUser.newWithoutValidation(
        UserId(1),
        Email.newWithoutValidation("dummy@example.com"),
        Username.newWithoutValidation("dummy-name"),
        Bio.newWithoutValidation("dummy-bio"),
        Image.newWithoutValidation("dummy-image")
    )
    @Test
    fun `更新したい正常なプロパティが1つ以上あり、残りがNullの場合、UpdatableRegisteredUserが戻り値となる`() {
        val differentValidUsername = "John"
        val actual = UpdatableRegisteredUser.new(validatedRegisteredUser, null, differentValidUsername, null, null)
        assertThat(actual.isValid).isTrue
    }

    @Test
    fun `プロパティが全てNullの場合、「更新できるプロパティが無い」旨のバリデーションエラーが戻り値となる`() {
        val actual = UpdatableRegisteredUser.new(validatedRegisteredUser, null, null, null, null)
        val expected = UpdatableRegisteredUser.ValidationError.NothingAttributeToUpdatable.invalidNel()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `更新したいプロパティが更新前と同じ場合、「更新できるプロパティが無い」旨のバリデーションエラーが戻り値となる`() {
        val validUsername = validatedRegisteredUser.username.value
        val actual = UpdatableRegisteredUser.new(validatedRegisteredUser, null, validUsername, null, null)
        val expected = UpdatableRegisteredUser.ValidationError.NothingAttributeToUpdatable.invalidNel()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `不正なプロパティが1つ以上ある場合、プロパティのバリデーションエラーが戻り値となる`() {
        val invalidEmail = "invalid-email-format"
        val actual = UpdatableRegisteredUser.new(validatedRegisteredUser, invalidEmail, null, null, null)
        val expected = Email.ValidationError.InvalidFormat(invalidEmail).invalidNel()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `正常と不正なプロパティが混ざっている場合、プロパティのバリデーションエラーが戻り値となる`() {
        val invalidEmail = "invalid-email-format"
        val differentValidUsername = "John"
        val actual = UpdatableRegisteredUser.new(validatedRegisteredUser, invalidEmail, differentValidUsername, null, null)
        val expected = Email.ValidationError.InvalidFormat(invalidEmail).invalidNel()
        assertThat(actual).isEqualTo(expected)
    }
}
