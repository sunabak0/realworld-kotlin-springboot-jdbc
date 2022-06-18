package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.invalid
import arrow.core.invalidNel
import arrow.core.valid
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.StringLength
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PasswordTest {
    @Test
    fun Userのpasswordは必須項目() {
        val invalid = Password.ValidationError.Required.check(null)
        val valid = Password.ValidationError.Required.check("")
        assertThat(invalid).isEqualTo(Password.ValidationError.Required.invalid())
        assertThat(valid).isEqualTo("".valid())
    }
    @Property(tries = 100)
    fun Userのpasswordは8文字以上(
        @ForAll @StringLength(max = 7) invalidPassword: String,
        @ForAll @StringLength(min = 8) validPassword: String,
    ) {
        val invalid = Password.ValidationError.TooShort.check(invalidPassword)
        val valid = Password.ValidationError.TooShort.check(validPassword)
        assertThat(invalid).isEqualTo(Password.ValidationError.TooShort(invalidPassword).invalidNel())
        assertThat(valid).isEqualTo(Unit.valid())
    }
    @Property(tries = 100)
    fun Userのpasswordは32文字以下(
        @ForAll @StringLength(min = 33) invalidPassword: String,
        @ForAll @StringLength(max = 32) validPassword: String,
    ) {
        val invalid = Password.ValidationError.TooLong.check(invalidPassword)
        val valid = Password.ValidationError.TooLong.check(validPassword)
        assertThat(invalid).isEqualTo(Password.ValidationError.TooLong(invalidPassword).invalidNel())
        assertThat(valid).isEqualTo(Unit.valid())
    }
}
