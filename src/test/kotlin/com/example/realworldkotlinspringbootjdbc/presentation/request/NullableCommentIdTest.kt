package com.example.realworldkotlinspringbootjdbc.presentation.request

import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.IntRange
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NullableCommentIdTest {
    @Nested
    class `引数の種類よって戻り値の型を確認する` {
        @Property
        fun `正常系-整数の場合、戻り値は整数`(
            @ForAll @IntRange(min = Int.MIN_VALUE, max = Int.MAX_VALUE) intNumber: Int
        ) {
            /**
             * given:
             */
            val pathParam = intNumber.toString()

            /**
             * when:
             */
            val actual = NullableCommentId.from(pathParam)

            /**
             * then:
             */
            Assertions.assertThat(actual).isEqualTo(intNumber)
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
            val pathParam = null
            val actual = NullableCommentId.from(pathParam)
            val expected = null
            Assertions.assertThat(actual).isEqualTo(expected)
        }
    }
}
