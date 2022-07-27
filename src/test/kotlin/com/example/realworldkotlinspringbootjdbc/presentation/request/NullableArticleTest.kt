package com.example.realworldkotlinspringbootjdbc.presentation.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream

class NullableArticleTest {
    private data class TestCase(
        val title: String,
        val rawRequstBody: String?,
        val expected: NullableArticle
    )

    @TestFactory
    fun test(): Stream<DynamicNode> {
        return Stream.of(
            TestCase(
                title = "引数が null の場合、メンバが全て null の NullableArticle が取得できる",
                rawRequstBody = null,
                expected = NullableArticle(
                    title = null,
                    description = null,
                    body = null,
                    tagList = null
                )
            ),
            TestCase(
                title = "JSON のフォーマットがおかしい場合、メンバが全て null の NullableArticle が取得できる",
                rawRequstBody = """
                    {
                        "article": {}...
                    }
                """.trimIndent(),
                expected = NullableArticle(
                    title = null,
                    description = null,
                    body = null,
                    tagList = null
                )
            ),
            TestCase(
                title = "引数のJSON文字列が'user'のobjectを持つが、値が空のオブジェクトの場合、メンバが全て null の NullableArticle が取得できる",
                rawRequstBody = """
                    {
                        "article": {}
                    }
                """.trimIndent(),
                expected = NullableArticle(
                    title = null,
                    description = null,
                    body = null,
                    tagList = null
                )
            ),
            TestCase(
                title = "プロパティが揃っている場合、全てのプロパティを持つ NullableArticle が取得できる",
                rawRequstBody = """
                    {
                        "article": {
                            "title": "dummy-title",
                            "description": "dummy-description",
                            "body": "dummy-body",
                            "tagList": ["dummy-tag-1", "dummy-tag-2"]
                        }
                    }
                """.trimIndent(),
                expected = NullableArticle(
                    title = "dummy-title",
                    description = "dummy-description",
                    body = "dummy-body",
                    tagList = listOf("dummy-tag-1", "dummy-tag-2")
                )
            ),
            TestCase(
                title = "想定していないプロパティがある場合、それを無視した NullableArticle が取得できる",
                rawRequstBody = """
                    {
                        "article": {
                            "must-ignore": true,
                            "title": "dummy-title",
                            "description": "dummy-description",
                            "body": "dummy-body",
                            "tagList": []
                        }
                    }
                """.trimIndent(),
                expected = NullableArticle(
                    title = "dummy-title",
                    description = "dummy-description",
                    body = "dummy-body",
                    tagList = listOf()
                )
            )
        ).map { testCase ->
            dynamicTest(testCase.title) {
                val actual = NullableArticle.from(testCase.rawRequstBody)
                assertThat(actual).isEqualTo(testCase.expected)
            }
        }
    }
}
