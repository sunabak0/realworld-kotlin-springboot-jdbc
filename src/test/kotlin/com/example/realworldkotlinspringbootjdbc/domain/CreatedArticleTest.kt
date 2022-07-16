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
}
