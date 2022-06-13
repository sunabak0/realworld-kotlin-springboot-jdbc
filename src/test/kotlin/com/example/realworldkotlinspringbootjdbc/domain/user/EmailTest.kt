package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.Validated
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class EmailTest {
    @Test
    fun `Userのemailは必須項目`() {
        val invalid = Email.ValidationError.Required.check(null)
        val valid = Email.ValidationError.Required.check("")
        Assertions.assertThat(invalid).isEqualTo(Validated.Invalid(Email.ValidationError.Required))
        Assertions.assertThat(valid).isEqualTo(Validated.Valid(""))
    }
    @Property
    fun `Userのemailはフォーマットが正しい必要がある`(
        @ForAll invalidEmail: String,
        // @ForAll @net.jqwik.web.api.Email(ipv4Host = true) validEmail: String,
    ) {
        val invalid = Email.ValidationError.InvalidFormat.check(invalidEmail)
        // val valid = Email.ValidationError.InvalidFormat.check(validEmail)
        Assertions.assertThat(invalid).isEqualTo(Validated.Invalid(Email.ValidationError.InvalidFormat(invalidEmail)))
        // Assertions.assertThat(valid).isEqualTo(Validated.Valid(validEmail))
    }
}
