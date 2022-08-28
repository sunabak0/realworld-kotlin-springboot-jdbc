package com.example.realworldkotlinspringbootjdbc.domain.article

import arrow.core.invalid
import arrow.core.invalidNel
import arrow.core.valid
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ArbitrarySupplier
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.StringLength
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DescriptionTest {
    @Test
    fun `CreatedArticle の Description は必須項目`() {
        val invalid = Description.ValidationError.Required.check(null)
        val valid = Description.ValidationError.Required.check("")
        assertThat(invalid).isEqualTo(Description.ValidationError.Required.invalid())
        assertThat(valid).isEqualTo("".valid())
    }

    @Property
    fun `CreatedArticle の Description の長さは 64 文字以下`(
        @ForAll @StringLength(min = 65) invalidDescription: String,
        @ForAll @StringLength(max = 64) validDescription: String,
    ) {
        val invalid = Description.ValidationError.TooLong.check(invalidDescription)
        val valid = Description.ValidationError.TooLong.check(validDescription)
        assertThat(invalid).isEqualTo(Description.ValidationError.TooLong(invalidDescription).invalidNel())
        assertThat(valid).isEqualTo(Unit.valid())
    }

    /**
     * Descriptionの有効な範囲のStringプロパティ
     */
    class DescriptionValidRange : ArbitrarySupplier<String> {
        override fun get(): Arbitrary<String> =
            Arbitraries.strings()
                .filter { !it.startsWith("diff-") }
    }
}
