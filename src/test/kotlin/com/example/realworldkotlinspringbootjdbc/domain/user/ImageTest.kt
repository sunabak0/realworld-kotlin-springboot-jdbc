package com.example.realworldkotlinspringbootjdbc.domain.user

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
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class ImageTest {
    class New {
        @Property
        fun `正常系-長さが有効な場合、検証済みのImageが戻り値`(
            @ForAll @From(supplier = ImageValidRange::class) validString: String,
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = Image.new(validString)

            /**
             * then:
             */
            when (actual) {
                is Invalid -> assert(false) { "原因: ${actual.value}" }
                is Valid -> Assertions.assertThat(actual.value.value).isEqualTo(validString)
            }
        }

        @Property
        fun `準正常系-長さが長すぎる場合、バリデーションエラーが戻り値`(
            @ForAll @StringLength(min = 513) tooLongString: String
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = Image.new(tooLongString)

            /**
             * then:
             */
            val expected = Image.ValidationError.TooLong(tooLongString).invalidNel()
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `準正常系-引数がnullの場合、バリデーションエラーが戻り値`() {
            /**
             * given:
             */
            val nullString = null

            /**
             * when:
             */
            val actual = Image.new(nullString)

            /**
             * then:
             */
            val expected = Image.ValidationError.Required.invalidNel()
            Assertions.assertThat(actual).isEqualTo(expected)
        }
    }

    /**
     * Imageの有効な範囲のStringプロパティ
     */
    class ImageValidRange : ArbitrarySupplier<String> {
        override fun get(): Arbitrary<String> =
            Arbitraries.strings()
                .ofMinLength(0)
                .ofMaxLength(512)
                .filter { !it.startsWith("diff-") }
    }
}
