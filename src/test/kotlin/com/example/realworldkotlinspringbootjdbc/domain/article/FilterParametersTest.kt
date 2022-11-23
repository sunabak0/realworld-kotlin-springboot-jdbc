package com.example.realworldkotlinspringbootjdbc.domain.article

import arrow.core.None
import arrow.core.Option
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.invalid
import arrow.core.nonEmptyListOf
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ArbitrarySupplier
import net.jqwik.api.ForAll
import net.jqwik.api.From
import net.jqwik.api.Property
import net.jqwik.api.constraints.WithNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FilterParametersTest {
    class NewTest {
        @Test
        fun `正常系-引数が全てnullの場合、フィルタ無しのフィルタ用パラメータが戻り値になる`() {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = FilterParameters.new(
                tag = null,
                author = null,
                favoritedByUsername = null,
            )

            /**
             * then:
             */
            when (actual) {
                is Invalid -> assert(false)
                is Valid -> {
                    val filterParameters = actual.value
                    assertThat(filterParameters.tag).isEqualTo(None)
                    assertThat(filterParameters.author).isEqualTo(None)
                    assertThat(filterParameters.favoritedByUsername).isEqualTo(None)
                    assertThat(filterParameters.limit).isEqualTo(20) // default: 20
                    assertThat(filterParameters.offset).isEqualTo(0) // default: 0
                }
            }
        }

        @Property
        fun `正常系-引数が想定内の場合、それぞれのフィルタ有りのフィルタ用パラメータが戻り値になる`(
            @ForAll @WithNull(value = 0.3) tag: String?,
            @ForAll @WithNull(value = 0.3) author: String?,
            @ForAll @WithNull(value = 0.3) favoritedByUsername: String?,
            @ForAll @From(supplier = ValidLimit::class) validLimit: Int,
            @ForAll @From(supplier = ValidOffset::class) validOffset: Int,
        ) {
            /**
             * given:
             * - tag: String?
             * - author: String?
             * - favoritedByUsername: String?
             * - limit: 有効範囲内のInt
             * - offset: 有効範囲内のInt
             */

            /**
             * when:
             */
            val actual = FilterParameters.new(
                tag = tag,
                author = author,
                favoritedByUsername = favoritedByUsername,
                limit = validLimit,
                offset = validOffset
            )

            /**
             * then:
             */
            when (actual) {
                is Invalid -> assert(false)
                is Valid -> {
                    val filterParameters = actual.value
                    assertThat(filterParameters.tag).isEqualTo(Option.fromNullable(tag))
                    assertThat(filterParameters.author).isEqualTo(Option.fromNullable(author))
                    assertThat(filterParameters.favoritedByUsername).isEqualTo(Option.fromNullable(favoritedByUsername))
                    assertThat(filterParameters.limit).isEqualTo(validLimit)
                    assertThat(filterParameters.offset).isEqualTo(validOffset)
                }
            }
        }

        @Property
        fun `準正常系-引数limitが有効範囲外の場合、その旨を表現するエラーのリストが戻り値になる`(
            @ForAll @From(supplier = UnderLimitMinimum::class) underLimitMinimum: Int,
            @ForAll @From(supplier = OverLimitMaximum::class) overLimitMaximum: Int,
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actualUnderLimitMinimum = FilterParameters.new(
                tag = null,
                author = null,
                favoritedByUsername = null,
                limit = underLimitMinimum,
            )
            val actualOverLimitMaximum = FilterParameters.new(
                tag = null,
                author = null,
                favoritedByUsername = null,
                limit = overLimitMaximum,
            )

            /**
             * then:
             */
            val expectedUnderLimitMinimum = nonEmptyListOf(
                FilterParameters.ValidationError.LimitError.RequireMinimumOrOver(underLimitMinimum)
            ).invalid()
            val expectedOverLimitMaximum = nonEmptyListOf(
                FilterParameters.ValidationError.LimitError.RequireMaximumOrUnder(overLimitMaximum)
            ).invalid()
            assertThat(actualUnderLimitMinimum).isEqualTo(expectedUnderLimitMinimum)
            assertThat(actualOverLimitMaximum).isEqualTo(expectedOverLimitMaximum)
        }

        @Property
        fun `準正常系-引数offsetが有効範囲外の場合、その旨を表現するエラーのリストが戻り値になる`(
            @ForAll @From(supplier = UnderOffsetMinimum::class) underOffsetMinimum: Int
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = FilterParameters.new(
                tag = null,
                author = null,
                favoritedByUsername = null,
                offset = underOffsetMinimum
            )

            /**
             * then:
             */
            val expected = nonEmptyListOf(
                FilterParameters.ValidationError.OffsetError.RequireMinimumOrOver(underOffsetMinimum)
            ).invalid()
            assertThat(actual).isEqualTo(expected)
        }

        /**
         * 有効範囲 内 の limit
         */
        class ValidLimit : ArbitrarySupplier<Int> {
            override fun get(): Arbitrary<Int> = Arbitraries.integers().between(1, 100)
        }
        /**
         * 下限値 未満 の limit
         */
        class UnderLimitMinimum : ArbitrarySupplier<Int> {
            override fun get(): Arbitrary<Int> = Arbitraries.integers().lessOrEqual(0)
        }
        /**
         * 上限値 超過 の limit
         */
        class OverLimitMaximum : ArbitrarySupplier<Int> {
            override fun get(): Arbitrary<Int> = Arbitraries.integers().greaterOrEqual(101)
        }
        /**
         * 有効範囲 内 の offset
         */
        class ValidOffset : ArbitrarySupplier<Int> {
            override fun get(): Arbitrary<Int> = Arbitraries.integers().between(0, Int.MAX_VALUE)
        }
        /**
         * 下限値 未満 の offset
         */
        class UnderOffsetMinimum : ArbitrarySupplier<Int> {
            override fun get(): Arbitrary<Int> = Arbitraries.integers().lessOrEqual(-1)
        }
    }
}
