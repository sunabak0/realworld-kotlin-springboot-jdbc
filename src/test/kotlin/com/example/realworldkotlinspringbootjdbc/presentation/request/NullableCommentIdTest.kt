package com.example.realworldkotlinspringbootjdbc.presentation.request

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NullableCommentIdTest {
    @Nested
    class `引数の種類よって null かどうか` {
        @Test
        fun `正の整数（0 以外）の場合`() {
            val pathParam = "1"
            val actual = NullableCommentId.from(pathParam)
            val expected = 1
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `0の場合`() {
            val pathParam = "0"
            val actual = NullableCommentId.from(pathParam)
            val expected = 0
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `負の整数の場合`() {
            val pathParam = "-1"
            val actual = NullableCommentId.from(pathParam)
            val expected = -1
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `小数点つきの正の数（0 以外）の場合`() {
            val pathParam = "1.0"
            val actual = NullableCommentId.from(pathParam)
            val expected = null
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `小数点つきの0の場合`() {
            val pathParam = "0.0"
            val actual = NullableCommentId.from(pathParam)
            val expected = null
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `小数点つきの負の整数の場合`() {
            val pathParam = "-1.0"
            val actual = NullableCommentId.from(pathParam)
            val expected = null
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `文字列の場合`() {
            val pathParam = "hoge"
            val actual = NullableCommentId.from(pathParam)
            val expected = null
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `null の場合`() {
            val pathParam = null
            val actual = NullableCommentId.from(pathParam)
            val expected = null
            Assertions.assertThat(actual).isEqualTo(expected)
        }
    }
}