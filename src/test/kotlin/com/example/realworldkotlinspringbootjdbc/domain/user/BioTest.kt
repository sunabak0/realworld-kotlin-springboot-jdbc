package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.invalid
import arrow.core.invalidNel
import arrow.core.valid
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.StringLength
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class BioTest {
    @Test
    fun Userのbioは必須項目() {
        val invalid = Bio.ValidationError.Required.check(null)
        val valid = Bio.ValidationError.Required.check("")
        Assertions.assertThat(invalid).isEqualTo(Bio.ValidationError.Required.invalid())
        Assertions.assertThat(valid).isEqualTo("".valid())
    }
    @Property(tries = 100)
    fun Userのbioは512文字以下(
        @ForAll @StringLength(min = 513) invalidBio: String,
        @ForAll @StringLength(max = 512) validBio: String,
    ) {
        val invalid = Bio.ValidationError.TooLong.check(invalidBio)
        val valid = Bio.ValidationError.TooLong.check(validBio)
        Assertions.assertThat(invalid).isEqualTo(Bio.ValidationError.TooLong(invalidBio).invalidNel())
        Assertions.assertThat(valid).isEqualTo(Unit.valid())
    }
}
