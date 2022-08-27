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

class BodyTest {
    @Test
    fun `CreatedArticle の Body は必須項目`() {
        val invalid = Body.ValidationError.Required.check(null)
        val valid = Body.ValidationError.Required.check("")
        assertThat(invalid).isEqualTo(Body.ValidationError.Required.invalid())
        assertThat(valid).isEqualTo("".valid())
    }

    @Property(tries = 100)
    fun `CreatedArticle の Body の長さは 1024 文字以下`(
        @ForAll @StringLength(min = 1025) invalidBody: String,
        @ForAll @StringLength(max = 1024) validBody: String,
    ) {
        val invalid = Body.ValidationError.TooLong.check(invalidBody)
        val valid = Body.ValidationError.TooLong.check(validBody)
        assertThat(invalid).isEqualTo(Body.ValidationError.TooLong(invalidBody).invalidNel())
        assertThat(valid).isEqualTo(Unit.valid())
    }

    /**
     * Bodyの有効な範囲のStringプロパティ
     */
    class BodyValidRange : ArbitrarySupplier<String> {
        override fun get(): Arbitrary<String> =
            Arbitraries.strings()
                .filter { !it.startsWith("diff-") }
    }
}
