package com.example.realworldkotlinspringbootjdbc.presentation.request

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NullableCommentTest {
    @Test
    fun `引数が null の場合、メンバが全て null の NullableComment が取得できる`() {
        val actual = NullableComment.from(null)
        val expected = NullableComment(null)
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `JSON のフォーマットがおかしい場合、メンバが全て null の NullableComment が取得できる`() {
        val json = """
            {
                "comment": {}...
            }
        """.trimIndent()
        val actual = NullableComment.from(json)
        val expected = NullableComment(null)
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    @Nested
    class `引数の JSON 文字列が "comment" を key に持つ場合` {
        @Test
        fun `残りが空っぽの場合、メンバが全て null の NullableComment が取得できる`() {
            val json = """
                {
                    "comment": {}
                }
            """.trimIndent()
            val actual = NullableComment.from(json)
            val expected = NullableComment(null)
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `プロパティが揃っている場合、全てのプロパティを持つ NullableComment が取得できる`() {
            val body = "dummy-body"
            val json = """
                {
                    "comment": {
                        "body": "$body"
                    }
                }
            """.trimIndent()
            val actual = NullableComment.from(json)
            val expected = NullableComment(body)
            Assertions.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `想定していないプロパティがあっても無視した NullableComment が取得できる`() {
            val body = "dummy-body"
            val json = """
                {
                    "comment": {
                        "must-ignore": "must-ignore",
                        "body": "$body"
                    }
                }
            """.trimIndent()
            val actual = NullableComment.from(json)
            val expected = NullableComment(body)
            Assertions.assertThat(actual).isEqualTo(expected)
        }
    }
}