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

class TitleTest {
    class New {
        @Property
        fun `正常系-長さが有効な場合、検証済みのTitleが戻り値`(
            @ForAll @From(supplier = TitleValidRange::class) validString: String,
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = Title.new(validString)

            /**
             * then:
             */
            when (actual) {
                is Invalid -> assert(false) { "原因: ${actual.value}" }
                is Valid -> assertThat(actual.value.value).isEqualTo(validString)
            }
        }

        @Property
        fun `準正常系-長さが有効でない場合、バリデーションエラーが戻り値`(
            @ForAll @StringLength(min = 33) tooLongString: String
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = Title.new(tooLongString)

            /**
             * then:
             */
            val expected = Title.ValidationError.TooLong(tooLongString).invalidNel()
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
            val actual = Title.new(nullString)

            /**
             * then:
             */
            val expected = Title.ValidationError.Required.invalidNel()
            assertThat(actual).isEqualTo(expected)
        }
    }

    /**
     * Titleの有効な範囲のStringプロパティ
     */
    class TitleValidRange : ArbitrarySupplier<String> {
        override fun get(): Arbitrary<String> =
            Arbitraries.strings()
                .ofMinLength(0)
                .ofMaxLength(32)
                .filter { !it.startsWith("diff-") }
    }
}
