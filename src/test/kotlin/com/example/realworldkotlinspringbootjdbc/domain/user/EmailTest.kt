package com.example.realworldkotlinspringbootjdbc.domain.user

import arrow.core.Invalid
import arrow.core.Valid
import arrow.core.invalidNel
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ArbitrarySupplier
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.From
import net.jqwik.api.Property
import net.jqwik.api.constraints.AlphaChars
import net.jqwik.api.constraints.NumericChars
import net.jqwik.api.constraints.Whitespace
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class EmailTest {
    class New {
        @Property(tries = 1000)
        fun `正常系-フォーマットが有効な場合、検証済みのEmailが戻り値`(
            @ForAll @From(supplier = EmailValidRange::class) validString: String,
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = Email.new(validString)

            /**
             * then:
             * 中身が引数そのものであること
             */
            when (actual) {
                is Invalid -> assert(false) { "原因: ${actual.value}" }
                is Valid -> assertThat(actual.value.value).isEqualTo(validString)
            }
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "+@a.Z",
                "%@0.0",
                "-----@A-----.A-----",
                ".....@0-----.9-----",
            ]
        )
        fun `正常系-境界値-フォーマットが有効な場合、検証済みのEmailが戻り値`(validString: String) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = Email.new(validString)

            /**
             * then:
             * 中身が引数そのものであること
             */
            when (actual) {
                is Invalid -> assert(false) { "原因: ${actual.value}" }
                is Valid -> assertThat(actual.value.value).isEqualTo(validString)
            }
        }

        @Property
        fun `準正常系-フォーマットが有効でない場合、バリデーションエラーが戻り値`(
            @ForAll @AlphaChars @NumericChars @Whitespace invalidString: String
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = Email.new(invalidString)

            /**
             * then:
             */
            val expected = Email.ValidationError.InvalidFormat(invalidString).invalidNel()
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
            val actual = Email.new(nullString)

            /**
             * then:
             */
            val expected = Email.ValidationError.Required.invalidNel()
            assertThat(actual).isEqualTo(expected)
        }
    }

    /**
     * Emailの有効な形式のStringプロパティ
     *
     * 以下のリンク先の正規表現を参考に作っている
     * https://android.googlesource.com/platform/frameworks/base/+/81aa097/core/java/android/util/Patterns.java#146
     */
    class EmailValidRange : ArbitrarySupplier<String> {
        override fun get(): Arbitrary<String> {
            val part1 = Arbitraries.strings()
                .numeric()
                .alpha()
                .withChars('+', '.', '_', '%', '-')
                .ofMinLength(1)
                .ofMaxLength(256)
            val part2 = Arbitraries.strings()
                .numeric()
                .alpha()
                .ofLength(1)
            val part3 = Arbitraries.strings()
                .numeric()
                .alpha()
                .withChars('-')
                .ofMinLength(0)
                .ofMaxLength(63)
            val part4 = Arbitraries.strings()
                .numeric()
                .alpha()
                .ofLength(1)
            val part5 = Arbitraries.strings()
                .numeric()
                .alpha()
                .ofMinLength(0)
                .ofMaxLength(24)
            /**
             * Kotlinではasが予約語であるため、`as` としている
             */
            return Combinators.combine(part1, part2, part3, part4, part5)
                .`as` { a, b, c, d, e -> "$a@$b$c.$d$e" }
                .filter { !it.startsWith("diff-") }
        }
    }
}
