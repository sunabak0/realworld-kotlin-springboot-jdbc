package com.example.realworldkotlinspringbootjdbc.domain.article.filter_parameters

import arrow.core.None
import arrow.core.Some
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.invalid
import arrow.core.nonEmptyListOf
import com.example.realworldkotlinspringbootjdbc.domain.article.FilterParameters
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.PropertyDefaults
import net.jqwik.api.constraints.AlphaChars
import net.jqwik.api.constraints.IntRange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FilterParametersTest {
    @Nested
    @PropertyDefaults(tries = 100)
    @DisplayName("FactoryMethod-new")
    class NewTest {
        @Test
        fun `正常系-引数が全てnullの場合、フィルタ無しのフィルタ用パラメータが戻り値になる`() {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = FilterParameters.new(
                tag = null,
                author = null,
                favoritedByUsername = null,
                limit = null,
                offset = null
            )

            /**
             * then:
             */
            when (actual) {
                is Invalid -> assert(false)
                is Valid -> {
                    val filterParameters = actual.value
                    assertThat(filterParameters.tag).isEqualTo(None)
                    assertThat(filterParameters.author).isEqualTo(None)
                    assertThat(filterParameters.favoritedByUsername).isEqualTo(None)
                    assertThat(filterParameters.limit.value).isEqualTo(20) // default: 20
                    assertThat(filterParameters.offset.value).isEqualTo(0) // default: 0
                }
            }
        }

        @Property
        fun `正常系-引数が全てnullではなく正しい場合、それぞれのフィルタ有りのフィルタ用パラメータが戻り値になる`(
            @ForAll tag: String,
            @ForAll author: String,
            @ForAll favoritedByUsername: String,
            @ForAll @IntRange(min = 1, max = 100) limit: Int,
            @ForAll @IntRange(min = 0, max = Int.MAX_VALUE) offset: Int,
        ) {
            /**
             * given:
             * - tag: nullではないString
             * - author: nullではないString
             * - favoritedByUsername: nullではないString
             * - limit: 範囲内のString
             * - offset: 範囲内のString
             */
            val limitString = limit.toString()
            val offsetString = offset.toString()

            /**
             * when:
             */
            val actual = FilterParameters.new(
                tag = tag,
                author = author,
                favoritedByUsername = favoritedByUsername,
                limit = limitString,
                offset = offsetString
            )

            /**
             * then:
             */
            when (actual) {
                is Invalid -> assert(false)
                is Valid -> {
                    val filterParameters = actual.value
                    assertThat(filterParameters.tag).isEqualTo(Some(tag))
                    assertThat(filterParameters.author).isEqualTo(Some(author))
                    assertThat(filterParameters.favoritedByUsername).isEqualTo(Some(favoritedByUsername))
                    assertThat(filterParameters.limit.value).isEqualTo(limit)
                    assertThat(filterParameters.offset.value).isEqualTo(offset)
                }
            }
        }

        @Property
        fun `準正常系-引数limit,offsetがバリデーションエラーを起こす場合、その旨を表現するエラーのリストが戻り値になる`(
            @ForAll @AlphaChars limit: String,
            @ForAll @AlphaChars offset: String,
        ) {
            /**
             * given:
             * - limit: アルファベット(Intに変換できない)
             * - offset: アルファベット(Intに変換できない)
             */

            /**
             * when:
             */
            val actual = FilterParameters.new(
                tag = null,
                author = null,
                favoritedByUsername = null,
                limit = limit,
                offset = offset
            )

            /**
             * then:
             */
            val expected = nonEmptyListOf(
                Limit.ValidationError.FailedConvertToInteger(limit),
                Offset.ValidationError.FailedConvertToInteger(offset)
            ).invalid()
            assertThat(actual).isEqualTo(expected)
        }
    }
}
