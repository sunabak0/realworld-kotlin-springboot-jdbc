package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Invalid
import arrow.core.Valid
import com.example.realworldkotlinspringbootjdbc.domain.article.BodyTest
import com.example.realworldkotlinspringbootjdbc.domain.article.DescriptionTest
import com.example.realworldkotlinspringbootjdbc.domain.article.TagTest
import com.example.realworldkotlinspringbootjdbc.domain.article.TitleTest
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
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
    }
}
