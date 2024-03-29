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

class SlugTest {
    class New {
        @Property
        fun `正常系-長さが有効な場合、検証済みのSlugが戻り値`(
            @ForAll @From(supplier = SlugValidRange::class) validString: String,
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = Slug.new(validString)

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
            val actual = Slug.new(tooLongString)

            /**
             * then:
             */
            val expected = Slug.ValidationError.TooLong(tooLongString).invalidNel()
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
            val actual = Slug.new(nullString)

            /**
             * then:
             */
            val expected = Slug.ValidationError.Required.invalidNel()
            assertThat(actual).isEqualTo(expected)
        }

        @Property
        fun `new() （引数なし）で生成される文字列は32文字の英数字`() {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = Slug.new().value

            /**
             * then:
             * - 32 文字の英数字
             */
            val expectedPattern = "^[a-z0-9]{32}$"
            assertThat(actual).matches(expectedPattern)
        }
    }

    /**
     * Slugの有効な範囲のStringプロパティ
     */
    class SlugValidRange : ArbitrarySupplier<String> {
        override fun get(): Arbitrary<String> =
            Arbitraries.strings()
                .ofMinLength(0)
                .ofMaxLength(32)
                .filter { !it.startsWith("diff-") }
    }
}
