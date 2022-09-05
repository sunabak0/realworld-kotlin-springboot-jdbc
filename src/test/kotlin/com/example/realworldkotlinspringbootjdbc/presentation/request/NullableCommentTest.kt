package com.example.realworldkotlinspringbootjdbc.presentation.request

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream

class NullableCommentTest {
    private data class TestCase(
        val title: String,
        val rawRequestBody: String?,
        val expected: NullableComment
    )

    @TestFactory
    fun test(): Stream<DynamicNode> {
        return Stream.of(
            TestCase(
                title = "引数が null の場合、メンバが全て null の NullableComment が取得できる",
                rawRequestBody = null,
                expected = NullableComment(
                    body = null
                )
            )
        ).map { testCase ->
            dynamicTest(testCase.title) {
                /**
                 * given:
                 */

                /**
                 * when:
                 */
                val actual = NullableComment.from(testCase.rawRequestBody)

                /**
                 * then:
                 */
                assertThat(actual).isEqualTo(testCase.expected)
            }
        }
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
