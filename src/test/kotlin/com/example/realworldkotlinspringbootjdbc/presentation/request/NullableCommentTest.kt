package com.example.realworldkotlinspringbootjdbc.presentation.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
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
            ),
            TestCase(
                title = "JSON のフォーマットがおかしい場合、メンバが全て null の NullableComment が取得できる",
                rawRequestBody = """
                    {
                        "comment": {}...
                    }
                """.trimIndent(),
                expected = NullableComment(
                    body = null
                )
            ),
            TestCase(
                title = "引数の JSON 文字列が \"comment\" を key に持ち、残りが空の場合メンバが全て null の NullableComment が取得できる",
                rawRequestBody = """
                    {
                        "comment": {}
                    }
                """.trimIndent(),
                expected = NullableComment(
                    body = null
                )
            ),
            TestCase(
                title = "引数の JSON 文字列が \"comment\" を key に持ち、プロパティが揃っている場合、全てのプロパティを持つ NullableComment が取得できる",
                rawRequestBody = """
                    {
                        "comment": {
                            "body": "dummy-body"
                        }
                    }
                """.trimIndent(),
                expected = NullableComment("dummy-body")
            ),
            TestCase(
                title = "引数の JSON 文字列が \"commen\" を key に持ち、想定していないプロパティがあっても無視した NullableComment が取得できる",
                rawRequestBody = """
                    {
                        "comment": {
                            "must-ignore": "must-ignore",
                            "body": "dummy-body"
                        }
                    }
                """.trimIndent(),
                expected = NullableComment("dummy-body")
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
}
