package com.example.realworldkotlinspringbootjdbc.domain.article

import arrow.core.Invalid
import arrow.core.Valid
import arrow.core.invalidNel
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ArbitrarySupplier
import net.jqwik.api.ForAll
import net.jqwik.api.From
import net.jqwik.api.Property
import net.jqwik.api.constraints.StringLength
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DescriptionTest {
    @Property
    fun `正常系-長さが有効な場合、検証済みの作成済み記事のDescriptionが戻り値`(
        @ForAll @From(supplier = DescriptionValidRange::class) validString: String
    ) {
        /**
         * given:
         */

        /**
         * when:
         */
        val actual = Description.new(validString)

        /**
         * then:
         */
        when (actual) {
            is Invalid -> assert(false) { "原因: ${actual.value}" }
            is Valid -> assertThat(actual.value.value).isEqualTo(validString)
        }
    }

    @Property
    fun `準正常系-長さが長すぎる場合、バリデーションエラーが戻り値`(
        @ForAll @StringLength(min = 65) tooLongString: String
    ) {
        /**
         * given:
         */

        /**
         * when:
         */
        val actual = Description.new(tooLongString)

        /**
         * then:
         */
        val expected = Description.ValidationError.TooLong(tooLongString).invalidNel()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `準正常系-nullの場合、バリデーションエラーが戻り値`() {
        /**
         * given:
         */
        val nullString = null

        /**
         * when:
         */
        val actual = Description.new(nullString)

        /**
         * then:
         */
        val expected = Description.ValidationError.Required.invalidNel()
        assertThat(actual).isEqualTo(expected)
    }

    /**
     * Descriptionの有効な範囲のStringプロパティ
     */
    class DescriptionValidRange : ArbitrarySupplier<String> {
        override fun get(): Arbitrary<String> =
            Arbitraries.strings()
                .ofMinLength(0)
                .ofMaxLength(64)
                .filter { !it.startsWith("diff-") }
    }
}
