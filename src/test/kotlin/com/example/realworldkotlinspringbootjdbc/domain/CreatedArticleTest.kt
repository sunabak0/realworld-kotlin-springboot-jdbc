package com.example.realworldkotlinspringbootjdbc.domain

import com.example.realworldkotlinspringbootjdbc.domain.article.Body
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.article.TagTest
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import net.jqwik.api.ForAll
import net.jqwik.api.From
import net.jqwik.api.Property
import net.jqwik.api.constraints.Size
import net.jqwik.api.constraints.UniqueElements
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
        @Property
        fun `正常系-持っているタグを指定した場合、trueが戻り値`(
            @ForAll @Size(min = 1, max = 10) @UniqueElements tagStringList: List<@From(supplier = TagTest.TagValidRange::class) String>
        ) {
            /**
             * given:
             * - 持っているTag
             * - タグリスト
             * - そのタグリストを持つ作成済み記事
             */
            val hadTag = Tag.newWithoutValidation(tagStringList.first())
            val tagList = tagStringList.map { Tag.newWithoutValidation(it) }.shuffled()
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
            val actual = createdArticle.hasTag(hadTag)

            /**
             * then:
             */
            assertThat(actual).isTrue
        }

        @Property
        fun `正常系-持っていないタグを指定した場合、falseが戻り値`(
            @ForAll @Size(min = 0, max = 10) @UniqueElements tagStringList: List<@From(supplier = TagTest.TagValidRange::class) String>
        ) {
            /**
             * given:
             * - 持っていないTag(TagValidRangeは"diff-"から始まるやつは出ない)
             * - タグリスト
             * - そのタグリストを持つ作成済み記事
             */
            val notHaveTag = Tag.newWithoutValidation("diff-tag")
            val tagList = tagStringList.map { Tag.newWithoutValidation(it) }.shuffled()
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
            val actual = createdArticle.hasTag(notHaveTag)

            /**
             * then:
             */
            assertThat(actual).isFalse
        }
    }
}
