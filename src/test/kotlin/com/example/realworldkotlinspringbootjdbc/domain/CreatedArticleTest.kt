package com.example.realworldkotlinspringbootjdbc.domain

import com.example.realworldkotlinspringbootjdbc.domain.article.Body
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.text.SimpleDateFormat
import java.util.stream.Stream

class CreatedArticleTest {
    data class TestCase(
        val title: String,
        val createdArticle: CreatedArticle,
        val otherCreatedArticle: CreatedArticle,
        val expected: Boolean
    )

    @TestFactory
    fun articleEqualsTest(): Stream<DynamicNode> {
        val date1 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
        val date2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-02T00:00:00+09:00")
        return Stream.of(
            TestCase(
                "ArticleId が一致する場合、他のプロパティが異なっていても、true を戻す",
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(1),
                    title = Title.newWithoutValidation("dummy-title"),
                    slug = Slug.newWithoutValidation("dummy-slug"),
                    body = Body.newWithoutValidation("dummy-body"),
                    createdAt = date1,
                    updatedAt = date1,
                    description = Description.newWithoutValidation("dummy-description"),
                    tagList = listOf(),
                    authorId = UserId(1),
                    favorited = false,
                    favoritesCount = 0
                ),
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(1),
                    title = Title.newWithoutValidation("other-dummy-title"),
                    slug = Slug.newWithoutValidation("dummy-slug"),
                    body = Body.newWithoutValidation("other-dummy-body"),
                    createdAt = date1,
                    updatedAt = date2,
                    description = Description.newWithoutValidation("dummy-description"),
                    tagList = listOf(),
                    authorId = UserId(1),
                    favorited = true,
                    favoritesCount = 1
                ),
                expected = true
            ),
            TestCase(
                "ArticleId が一致しない場合、他のプロパティが全て同じでも、false を戻す",
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(1),
                    title = Title.newWithoutValidation("dummy-title"),
                    slug = Slug.newWithoutValidation("dummy-slug"),
                    body = Body.newWithoutValidation("dummy-body"),
                    createdAt = date1,
                    updatedAt = date1,
                    description = Description.newWithoutValidation("dummy-description"),
                    tagList = listOf<Tag>(),
                    authorId = UserId(1),
                    favorited = false,
                    favoritesCount = 0
                ),
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(2),
                    title = Title.newWithoutValidation("dummy-title"),
                    slug = Slug.newWithoutValidation("dummy-slug"),
                    body = Body.newWithoutValidation("dummy-body"),
                    createdAt = date1,
                    updatedAt = date1,
                    description = Description.newWithoutValidation("dummy-description"),
                    tagList = listOf<Tag>(),
                    authorId = UserId(1),
                    favorited = false,
                    favoritesCount = 0
                ),
                expected = false
            )
        ).map { testCase ->
            dynamicTest(testCase.title) {
                assertThat(testCase.createdArticle == testCase.otherCreatedArticle).isEqualTo(testCase.expected)
            }
        }
    }

    class HasTag {
        data class TestCase(
            val title: String,
            val targetTagString: String,
            val stringTags: List<String>,
            val expected: Boolean
        )

        @TestFactory
        fun hasTagTest(): Stream<DynamicNode> =
            Stream.of(
                TestCase(
                    title = "正常系-持っているタグを指定した場合、trueが戻り値",
                    targetTagString = "F#",
                    stringTags = listOf("F#"),
                    expected = true
                ),
                TestCase(
                    title = "正常系-持っているタグを指定した場合、trueが戻り値",
                    targetTagString = "Rust",
                    stringTags = listOf("F#", "Rust", "Kotlin"),
                    expected = true
                ),
                TestCase(
                    title = "正常系-持っているタグを指定した場合、trueが戻り値",
                    targetTagString = "Kotlin",
                    stringTags = listOf("F#", "Rust", "Scala", "Kotlin"),
                    expected = true
                ),
                TestCase(
                    title = "正常系-持っていないタグを指定した場合、falseが戻り値",
                    targetTagString = "#F",
                    stringTags = listOf("F#", "#f"),
                    expected = false
                ),
                TestCase(
                    title = "正常系-持っていないタグを指定した場合、falseが戻り値",
                    targetTagString = "F#",
                    stringTags = emptyList(),
                    expected = false
                ),
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    /**
                     * given:
                     */
                    val targetTag = Tag.newWithoutValidation(testCase.targetTagString)
                    val tagList = testCase.stringTags.map { Tag.newWithoutValidation(it) }
                    val createdArticle = CreatedArticle.newWithoutValidation(
                        id = ArticleId(1),
                        title = Title.newWithoutValidation("dummy-title"),
                        slug = Slug.newWithoutValidation("dummy-slug"),
                        body = Body.newWithoutValidation("dummy-body"),
                        createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                        updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                        description = Description.newWithoutValidation("dummy-description"),
                        tagList = tagList,
                        authorId = UserId(1),
                        favorited = false,
                        favoritesCount = 0
                    )

                    /**
                     * when:
                     */
                    val actual = createdArticle.hasTag(targetTag)

                    /**
                     * then:
                     */
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
    }
}
