package com.example.realworldkotlinspringbootjdbc.presentation.request

import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.IntRange
import net.jqwik.api.constraints.Negative
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NullableCommentIdTest {
    @Nested
    class `引数の種類よって戻り値の型を確認する` {
        @Property
        fun `正常系-0 以上の整数の場合、戻り値は整数`(
            @ForAll @IntRange(min = 0, max = Int.MAX_VALUE) positiveNumberOrZero: Int
        ) {
            /**
             * given:
             */
            val pathParam = positiveNumberOrZero.toString()

            /**
             * when:
             */
            val actual = NullableCommentId.from(pathParam)

            /**
             * then:
             */
            Assertions.assertThat(actual).isEqualTo(positiveNumberOrZero)
        }

        @Test
        fun `0の場合、戻り値は整数`() {
            /**
             * given:
             */
            val pathParam = "0"

            /**
             * when:
             */
            val actual = NullableCommentId.from(pathParam)

            /**
             * then:
             */
            val expected = 0
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Property
        fun `負の整数の場合、戻り値は整数`(
            @ForAll @Negative negativeNumber: Int
        ) {
            /**
             * given:
             */
            val pathParam = negativeNumber.toString()

            /**
             * when:
             */
            val actual = NullableCommentId.from(pathParam)

            /**
             * then
             */
            Assertions.assertThat(actual).isEqualTo(negativeNumber)
        }

        @Test
        fun `小数点つきの正の数（0 以外）の場合、戻り値は null`() {
            val pathParam = "1.0"
            val actual = NullableCommentId.from(pathParam)
            val expected = null
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `小数点つきの0の場合、戻り値は null`() {
            val pathParam = "0.0"
            val actual = NullableCommentId.from(pathParam)
            val expected = null
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `小数点つきの負の整数の場合、戻り値は null`() {
            val pathParam = "-1.0"
            val actual = NullableCommentId.from(pathParam)
            val expected = null
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `文字列の場合、戻り値は null`() {
            val pathParam = "hoge"
            val actual = NullableCommentId.from(pathParam)
            val expected = null
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `null の場合、戻り値は null`() {
            val pathParam = null
            val actual = NullableCommentId.from(pathParam)
            val expected = null
            Assertions.assertThat(actual).isEqualTo(expected)
        }
    }
}
