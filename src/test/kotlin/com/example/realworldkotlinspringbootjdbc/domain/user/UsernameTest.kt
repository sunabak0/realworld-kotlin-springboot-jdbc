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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UsernameTest {
    class New {
        @Property
        fun `正常系-長さが有効な場合、検証済みのUsernameが戻り値`(
            @ForAll @From(supplier = UsernameValidRange::class) validString: String,
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = Username.new(validString)

            /**
             * then:
             */
            when (actual) {
                is Invalid -> assert(false) { "原因: ${actual.value}" }
                is Valid -> assertThat(actual.value.value).isEqualTo(validString)
            }
        }

        @Property
        fun `準正常系-長すぎる場合、バリデーションエラーが戻り値`(
            @ForAll @StringLength(min = 33) tooLongString: String
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = Username.new(tooLongString)

            /**
             * then:
             */
            val expected = Username.ValidationError.TooLong(tooLongString).invalidNel()
            assertThat(actual).isEqualTo(expected)
        }

        @Property
        fun `準正常系-短すぎる場合、バリデーションエラーが戻り値`(
            @ForAll @StringLength(max = 3) tooShortString: String
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = Username.new(tooShortString)

            /**
             * then:
             */
            val expected = Username.ValidationError.TooShort(tooShortString).invalidNel()
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
            val actual = Username.new(nullString)

            /**
             * then:
             */
            val expected = Username.ValidationError.Required.invalidNel()
            assertThat(actual).isEqualTo(expected)
        }
    }

    /**
     * Usernameの有効なStringプロパティ
     */
    class UsernameValidRange : ArbitrarySupplier<String> {
        override fun get(): Arbitrary<String> =
            Arbitraries.strings()
                .ofMinLength(4)
                .ofMaxLength(32)
                .filter { !it.startsWith("diff-") }
    }
}
