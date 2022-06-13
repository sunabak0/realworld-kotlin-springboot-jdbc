package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.Validated
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.StringLength
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class BioTest {
    @Test
    fun `Userのbioは必須項目`() {
        val invalid = Bio.ValidationError.Required.check(null)
        val valid = Bio.ValidationError.Required.check("")
        Assertions.assertThat(invalid).isEqualTo(Validated.Invalid(Bio.ValidationError.Required))
        Assertions.assertThat(valid).isEqualTo(Validated.Valid(""))
    }
    @Property(tries = 100)
    fun `Userのbioは512文字以下`(
        @ForAll @StringLength(min = 513) invalidBio: String,
        @ForAll @StringLength(max = 512) validBio: String,
    ) {
        val invalid = Bio.ValidationError.TooLong.check(invalidBio)
        val valid = Bio.ValidationError.TooLong.check(validBio)
        Assertions.assertThat(invalid).isEqualTo(Validated.Invalid(Bio.ValidationError.TooLong(invalidBio)))
        Assertions.assertThat(valid).isEqualTo(Validated.Valid(validBio))
    }
}
