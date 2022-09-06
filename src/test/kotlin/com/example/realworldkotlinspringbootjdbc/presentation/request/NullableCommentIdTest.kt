package com.example.realworldkotlinspringbootjdbc.presentation.request

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ArbitrarySupplier
import net.jqwik.api.ForAll
import net.jqwik.api.From
import net.jqwik.api.Property
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NullableCommentIdTest {
    @Nested
    class `引数の種類よって戻り値の型を確認する` {
        @Property
        fun `正常系-整数の場合、戻り値は整数`(
            @ForAll @From(supplier = NullableCommentIdValidRange::class) validInt: Int
        ) {
            /**
             * given:
             */
            val pathParam = validInt.toString()

            /**
             * when:
             */
            val actual = NullableCommentId.from(pathParam)

            /**
             * then:
             */
            Assertions.assertThat(actual).isEqualTo(validInt)
        }

        @Test
        fun `文字列の場合、戻り値は null`() {
            /**
             * given:
             */
            val pathParam = "dummy"

            /**
             * when:
             */
            val actual = NullableCommentId.from(pathParam)

            /**
             * then:
             */
            val expected = null
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `null の場合、戻り値は null`() {
            /**
             * given:
             */
            val pathParam = null

            /**
             * when:
             */
            val actual = NullableCommentId.from(pathParam)

            /**
             * then:
             */
            val expected = null
            Assertions.assertThat(actual).isEqualTo(expected)
        }
    }

    /**
     * NullableCommentId の有効な範囲の Int プロパティ
     */
    class NullableCommentIdValidRange : ArbitrarySupplier<Int> {
        override fun get(): Arbitrary<Int> =
            Arbitraries.integers()
                .between(Int.MIN_VALUE, Int.MAX_VALUE)
    }
}
