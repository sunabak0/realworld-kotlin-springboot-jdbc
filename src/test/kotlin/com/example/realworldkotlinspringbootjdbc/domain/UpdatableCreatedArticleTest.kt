package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Invalid
import arrow.core.Valid
import arrow.core.invalidNel
import com.example.realworldkotlinspringbootjdbc.domain.article.Body
import com.example.realworldkotlinspringbootjdbc.domain.article.BodyTest
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.DescriptionTest
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.article.TitleTest
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import net.jqwik.api.ForAll
import net.jqwik.api.From
import net.jqwik.api.Property
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.text.SimpleDateFormat
import java.util.stream.Stream

class UpdatableCreatedArticleTest {
    class New {
        private companion object {
            val originalCreatedArticle = CreatedArticle.newWithoutValidation(
                id = ArticleId(1),
                title = Title.newWithoutValidation("diff-original-title"),
                slug = Slug.newWithoutValidation("diff-original-slug"),
                body = Body.newWithoutValidation("diff-original-body"),
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                description = Description.newWithoutValidation("diff-original-description"),
                tagList = emptyList(),
                authorId = UserId(1),
                favorited = false,
                favoritesCount = 0,
            )
        }

        @Property
        fun `正常系-全て正しい場合、更新可能な作成済み記事が戻り値`(
            @ForAll @From(supplier = TitleTest.TitleValidRange::class) title: String,
            @ForAll @From(supplier = DescriptionTest.DescriptionValidRange::class) description: String,
            @ForAll @From(supplier = BodyTest.BodyValidRange::class) body: String,
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = UpdatableCreatedArticle.new(originalCreatedArticle, title, description, body)

            /**
             * then:
             */
            when (actual) {
                is Invalid -> assert(false) { "原因: ${actual.value}" }
                is Valid -> {
                    val updatableCreatedArticle = actual.value
                    val softly = SoftAssertions()
                    softly.assertThat(updatableCreatedArticle.title.value).isEqualTo(title)
                    softly.assertThat(updatableCreatedArticle.description.value).isEqualTo(description)
                    softly.assertThat(updatableCreatedArticle.body.value).isEqualTo(body)
                    softly.assertAll()
                }
            }
        }

        data class PartialNullTestCase(
            val testCaseTitle: String,
            val titleGivenThenPair: Pair<String?, String>, // <given, expected>
            val descriptionGivenThenPair: Pair<String?, String>, // <given, expected>
            val bodyGivenThenPair: Pair<String?, String>, // <given, expected>
        )

        @TestFactory
        fun `正常系-一部がnullでも、オリジナルの方が採用された更新可能な作成済み記事が戻り値`(): Stream<DynamicNode> =
            Stream.of(
                PartialNullTestCase(
                    testCaseTitle = "正常系-記事タイトルと記事概要がnullの場合、それぞれオリジナルの方が採用された、更新可能な作成済みの記事が戻り値",
                    titleGivenThenPair = Pair(null, originalCreatedArticle.title.value),
                    descriptionGivenThenPair = Pair(null, originalCreatedArticle.description.value),
                    bodyGivenThenPair = Pair("new-body", "new-body")
                ),
                PartialNullTestCase(
                    testCaseTitle = "正常系-本文がnullの場合、オリジナルの方が採用される",
                    titleGivenThenPair = Pair("new-title", "new-title"),
                    descriptionGivenThenPair = Pair("new-description", "new-description"),
                    bodyGivenThenPair = Pair(null, originalCreatedArticle.body.value)
                ),
            ).map { testCase ->
                dynamicTest(testCase.testCaseTitle) {
                    /**
                     * given:
                     */
                    val title = testCase.titleGivenThenPair.first
                    val description = testCase.descriptionGivenThenPair.first
                    val body = testCase.bodyGivenThenPair.first

                    /**
                     * when:
                     */
                    val actual = UpdatableCreatedArticle.new(originalCreatedArticle, title, description, body)

                    /**
                     * then:
                     */
                    val expectedTitle = testCase.titleGivenThenPair.second
                    val expectedDescription = testCase.descriptionGivenThenPair.second
                    val expectedBody = testCase.bodyGivenThenPair.second
                    when (actual) {
                        is Invalid -> assert(false) { "原因: ${actual.value}" }
                        is Valid -> {
                            val updatableCreatedArticle = actual.value
                            val softly = SoftAssertions()
                            softly.assertThat(updatableCreatedArticle.title.value).isEqualTo(expectedTitle)
                            softly.assertThat(updatableCreatedArticle.description.value).isEqualTo(expectedDescription)
                            softly.assertThat(updatableCreatedArticle.body.value).isEqualTo(expectedBody)
                            softly.assertAll()
                        }
                    }
                }
            }

        data class NothingDifferenceTestCase(
            val testCaseTitle: String,
            val title: String?,
            val description: String?,
            val body: String?,
        )

        @TestFactory
        fun `準正常系-差分がない場合、その旨を表現するエラーが戻り値`(): Stream<DynamicNode> =
            Stream.of(
                NothingDifferenceTestCase(
                    testCaseTitle = "準正常系-全てがnullの場合、差分がない旨のエラーが戻り値",
                    title = null,
                    description = null,
                    body = null
                ),
                NothingDifferenceTestCase(
                    testCaseTitle = "準正常系-オリジナルと全て差分がない場合、差分がない旨のエラーが戻り値",
                    title = originalCreatedArticle.title.value,
                    description = originalCreatedArticle.description.value,
                    body = originalCreatedArticle.body.value
                ),
            ).map { testCase ->
                dynamicTest(testCase.testCaseTitle) {
                    /**
                     * given:
                     */
                    val title = testCase.title
                    val description = testCase.description
                    val body = testCase.body

                    /**
                     * when:
                     */
                    val actual = UpdatableCreatedArticle.new(originalCreatedArticle, title, description, body)

                    /**
                     * then:
                     */
                    val expected = UpdatableCreatedArticle.ValidationError.NothingAttributeToUpdatable.invalidNel()
                    assertThat(actual).isEqualTo(expected)
                }
            }
    }
}
