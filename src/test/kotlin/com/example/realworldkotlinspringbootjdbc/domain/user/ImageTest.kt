package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.Validated
import arrow.core.invalidNel
import arrow.core.valid
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.StringLength
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class ImageTest {
    @Test
    fun Userのimageは必須項目() {
        val invalid = Image.ValidationError.Required.check(null)
        val valid = Image.ValidationError.Required.check("")
        Assertions.assertThat(invalid).isEqualTo(Validated.Invalid(Image.ValidationError.Required))
        Assertions.assertThat(valid).isEqualTo(Validated.Valid(""))
    }
    @Property(tries = 100)
    fun Userのimageは512文字以下(
        @ForAll @StringLength(min = 513) invalidImage: String,
        @ForAll @StringLength(max = 512) validImage: String,
    ) {
        val invalid = Image.ValidationError.TooLong.check(invalidImage)
        val valid = Image.ValidationError.TooLong.check(validImage)
        Assertions.assertThat(invalid).isEqualTo(Image.ValidationError.TooLong(invalidImage).invalidNel())
        Assertions.assertThat(valid).isEqualTo(Unit.valid())
    }
}
