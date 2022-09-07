package com.example.realworldkotlinspringbootjdbc.sandbox.kotlin

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * String.toIntOrNull() の sandbox テスト
 *
 */
class ToIntOrNull {
    @Nested
    class `小数部が 0 の小数の場合、戻り値は null` {
        val expected = null

        @Test
        fun `整数部が正の数の場合`() {
            /**
             * given:
             */
            val num = "1.0"

            /**
             * when:
             */
            val actual = num.toIntOrNull()

            /**
             * then
             */
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `整数部が 0 の場合`() {
            /**
             * given:
             */
            val num = "0.0"

            /**
             * when:
             */
            val actual = num.toIntOrNull()

            /**
             * then:
             */
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `整数部が負の数の場合`() {
            /**
             * given:
             */
            val num = "-1.0"

            /**
             * when:
             */
            val actual = num.toIntOrNull()

            /**
             * then:
             */
            Assertions.assertThat(actual).isEqualTo(expected)
        }
    }
}
