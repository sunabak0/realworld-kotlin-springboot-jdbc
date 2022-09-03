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
import net.jqwik.api.constraints.Chars
import net.jqwik.api.constraints.NotBlank
import net.jqwik.api.constraints.NotEmpty
import net.jqwik.api.constraints.StringLength
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TagTest {
    class New {
        @Property
        fun `正常系-長さが有効な場合、検証済みのTagが戻り値`(
            @ForAll @From(supplier = TagValidRange::class) validString: String
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
            @ForAll @NotBlank @StringLength(min = 17) tooLongString: String
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

        @Property
        fun `準正常系-空白のみの場合、バリデーションエラーが戻り値`(
            @ForAll @Chars(' ') @NotEmpty whiteSpaceOnlyTag: String
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = Tag.new(whiteSpaceOnlyTag)

            /**
             * then:
             * - RequiredNotBlankエラーがある
             */
            when (actual) {
                is Invalid -> {
                    val errors = actual.value
                    assertThat(errors).contains(Tag.ValidationError.RequiredNotBlank)
                }
                is Valid -> assert(false) { "バリデーションエラーになることを期待しています" }
            }
        }
    }

    /**
     * Tagの有効な範囲のStringプロパティ
     */
    class TagValidRange : ArbitrarySupplier<String> {
        override fun get(): Arbitrary<String> {
            return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(16)
                .filter { !it.startsWith("diff-") && it.isNotBlank() }
        }
    }

    /**
     * 有効な範囲のTagStringのList
     *
     * 背景
     *
     * ```kt
     * @ForAll tagStringList : List<@From(supplier=TagValidRange::class) String>
     * ```
     *
     * のようにgenericsの指定で@Fromを使うと、せっかくカスタマイズしたルールが外れて、使い物にならなかった
     *
     * なので、Listも定義してあげる
     * このクラス定義によって
     *
     * ```kt
     * @ForAll @From(supplier=TagValidedRangeList::class) tagStringList : List<String>
     * ```
     *
     * というように使うことができる
     */
    class TagValidedRangeList : ArbitrarySupplier<List<String>> {
        override fun get(): Arbitrary<List<String>> = TagValidRange().get().list().uniqueElements()
    }
}
