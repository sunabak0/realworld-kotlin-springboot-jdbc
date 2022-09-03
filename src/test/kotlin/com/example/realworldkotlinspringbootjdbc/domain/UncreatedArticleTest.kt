package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Invalid
import arrow.core.Valid
import arrow.core.nonEmptyListOf
import com.example.realworldkotlinspringbootjdbc.domain.article.Body
import com.example.realworldkotlinspringbootjdbc.domain.article.BodyTest
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.DescriptionTest
import com.example.realworldkotlinspringbootjdbc.domain.article.TagTest
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.article.TitleTest
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import net.jqwik.api.Example
import net.jqwik.api.ForAll
import net.jqwik.api.From
import net.jqwik.api.Property
import net.jqwik.api.constraints.Size
import org.assertj.core.api.Assertions.assertThat

class UncreatedArticleTest {
    class New {
        @Property
        fun `正常系-属性の値が全て有効な場合、検証済みの未作成記事が戻り値`(
            @ForAll @From(supplier = TitleTest.TitleValidRange::class) title: String,
            @ForAll @From(supplier = DescriptionTest.DescriptionValidRange::class) description: String,
            @ForAll @From(supplier = BodyTest.BodyValidRange::class) body: String,
            @ForAll @Size(min = 0, max = 10) @From(supplier = TagTest.TagValidedRangeList::class) tagStringList: List<String>
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = UncreatedArticle.new(
                title = title,
                description = description,
                body = body,
                tagList = tagStringList,
                authorId = UserId(1),
            )

            /**
             * then:
             * - Validであること
             * - それぞれの属性の値が引数の値となるか
             *   - 例: Title.valueはtitleかどうか
             */
            when (actual) {
                is Invalid -> assert(false) { "原因: ${actual.value}" }
                is Valid -> {
                    val article = actual.value
                    assertThat(article.title.value).isEqualTo(title)
                    assertThat(article.description.value).isEqualTo(description)
                    assertThat(article.body.value).isEqualTo(body)
                    assertThat(article.authorId.value).isEqualTo(1)
                    val articleTagStringList = article.tagList.map { it.value }
                    assertThat(articleTagStringList).hasSameElementsAs(tagStringList)
                }
            }
        }

        @Example
        fun `正常系-タグリストがnullでも、検証は通り、未作成記事が戻り値`(
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
            val actual = UncreatedArticle.new(
                title = title,
                description = description,
                body = body,
                tagList = null,
                authorId = UserId(1),
            )

            /**
             * then:
             * - Validであること
             *   - tagListは空List
             */
            when (actual) {
                is Invalid -> assert(false) { "原因: ${actual.value}" }
                is Valid -> {
                    val article = actual.value
                    assertThat(article.tagList).isEmpty()
                }
            }
        }

        @Property
        fun `準正常系-属性の値が1つでも有効でない場合、バリデーションエラーが戻り値`(
            @ForAll @From(supplier = TitleTest.TitleValidRange::class) title: String,
            @ForAll @From(supplier = DescriptionTest.DescriptionValidRange::class) description: String,
            @ForAll @From(supplier = BodyTest.BodyValidRange::class) body: String,
            @ForAll @Size(min = 0, max = 10) @From(supplier = TagTest.TagValidedRangeList::class) tagStringList: List<String>
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val oneNullActual = UncreatedArticle.new(
                title = null,
                description = description,
                body = body,
                tagList = tagStringList,
                authorId = UserId(1),
            )
            val twoNullActual = UncreatedArticle.new(
                title = title,
                description = null,
                body = null,
                tagList = tagStringList,
                authorId = UserId(1),
            )
            val threeNullActual = UncreatedArticle.new(
                title = null,
                description = null,
                body = body,
                tagList = null,
                authorId = UserId(1),
            )

            /**
             * then:
             * - エラーは1種類
             * - エラーは2種類
             * - エラーは2種類(TagListはnullでもエラーにならないため)
             */
            val oneNullExpected = nonEmptyListOf(
                Title.ValidationError.Required,
            )
            val twoNullExpected = nonEmptyListOf(
                Description.ValidationError.Required,
                Body.ValidationError.Required,
            )
            val threeNullExpected = nonEmptyListOf(
                Title.ValidationError.Required,
                Description.ValidationError.Required,
            )
            when (oneNullActual) {
                is Invalid -> {
                    val errors = oneNullActual.value
                    assertThat(errors).hasSameElementsAs(oneNullExpected)
                }
                is Valid -> assert(false) { "準正常系のテストなので、Invalidであることを期待します" }
            }
            when (twoNullActual) {
                is Invalid -> {
                    val errors = twoNullActual.value
                    assertThat(errors).hasSameElementsAs(twoNullExpected)
                }
                is Valid -> assert(false) { "準正常系のテストなので、Invalidであることを期待します" }
            }
            when (threeNullActual) {
                is Invalid -> {
                    val errors = threeNullActual.value
                    assertThat(errors).hasSameElementsAs(threeNullExpected)
                }
                is Valid -> assert(false) { "準正常系のテストなので、Invalidであることを期待します" }
            }
        }
    }
}
