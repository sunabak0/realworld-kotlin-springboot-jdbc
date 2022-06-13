package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.Validated
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.StringLength
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class UsernameTest {
    @Test
    fun `Userのusernameは必須項目`() {
        val invalid = Username.ValidationError.Required.check(null)
        val valid = Username.ValidationError.Required.check("")
        Assertions.assertThat(invalid).isEqualTo(Validated.Invalid(Username.ValidationError.Required))
        Assertions.assertThat(valid).isEqualTo(Validated.Valid(""))
    }
    @Property(tries = 100)
    fun `Userのusernameは4文字以上`(
        @ForAll @StringLength(max = 3) invalidUsername: String,
        @ForAll @StringLength(min = 4) validUsername: String,
    ) {
        val invalid = Username.ValidationError.TooShort.check(invalidUsername)
        val valid = Username.ValidationError.TooShort.check(validUsername)
        Assertions.assertThat(invalid).isEqualTo(Validated.Invalid(Username.ValidationError.TooShort(invalidUsername)))
        Assertions.assertThat(valid).isEqualTo(Validated.Valid(validUsername))
    }
    @Property(tries = 100)
    fun `Userのusernameは32文字以下`(
        @ForAll @StringLength(min = 33) invalidUsername: String,
        @ForAll @StringLength(max = 32) validUsername: String,
    ) {
        val invalid = Username.ValidationError.TooLong.check(invalidUsername)
        val valid = Username.ValidationError.TooLong.check(validUsername)
        Assertions.assertThat(invalid).isEqualTo(Validated.Invalid(Username.ValidationError.TooLong(invalidUsername)))
        Assertions.assertThat(valid).isEqualTo(Validated.Valid(validUsername))
    }
}
