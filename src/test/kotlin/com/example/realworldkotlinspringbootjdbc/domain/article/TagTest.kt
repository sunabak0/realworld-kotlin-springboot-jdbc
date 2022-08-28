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

class TagTest {
    class New {
        @Property
        fun `正常系-長さが有効な場合、検証済みのTagが戻り値`(
            @ForAll @From(supplier = TagValidRange::class) validString: String,
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = Tag.new(validString)

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
            @ForAll @StringLength(min = 17) tooLongString: String
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = Tag.new(tooLongString)

            /**
             * then:
             */
            val expected = Tag.ValidationError.TooLong(tooLongString).invalidNel()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `準正常系-長さが短すぎる場合、バリデーションエラーが戻り値`() {
            /**
             * given:
             */
            val tooShortString = ""

            /**
             * when:
             */
            val actual = Tag.new(tooShortString)

            /**
             * then:
             */
            val expected = Tag.ValidationError.TooShort(tooShortString).invalidNel()
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
            val actual = Tag.new(nullString)

            /**
             * then:
             */
            val expected = Tag.ValidationError.Required.invalidNel()
            assertThat(actual).isEqualTo(expected)
        }
    }

    /**
     * Tagの有効な範囲のStringプロパティ
     */
    class TagValidRange : ArbitrarySupplier<String> {
        override fun get(): Arbitrary<String> =
            Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(16)
                .filter { !it.startsWith("diff-") }
    }
}
