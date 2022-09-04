package com.example.realworldkotlinspringbootjdbc.domain.article

import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.invalidNel
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.IntRange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CommentIdTest {
    class NewTest {

        @Property(tries = 100)
        fun `正常系-自然数の場合`(
            @ForAll @IntRange(min = 1) naturalNumber: Int
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = CommentId.new(naturalNumber)

            /**
             * then:
             */
            when (actual) {
                is Invalid -> assert(false) { "原因: ${actual.value}" }
                is Valid -> assertThat(actual.value.value).isEqualTo(naturalNumber)
            }
        }

        @Test
        fun `準正常系-null の場合、バリデーションエラーが戻り値`() {
            /**
             * given:
             */
            val nullInt = null

            /**
             * when:
             */
            val actual = CommentId.new(nullInt)

            /**
             * then:
             */
            val expected = CommentId.ValidationError.Required.invalidNel()
            assertThat(actual).isEqualTo(expected)
        }

        @Property(tries = 100)
        fun `準正常系-自然数でない場合`(
            @ForAll @IntRange(min = 0) naturalNumber: Int
        ) {
            /**
             * given:
             * 0 以上の整数に -1 を乗算することで、自然数でない整数を表現する
             */
            val notNaturalNumber = -1 * naturalNumber

            /**
             * when:
             */
            val actual = CommentId.new(notNaturalNumber)

            /**
             * then:
             */
            val expected = CommentId.ValidationError.MustBeNaturalNumber(notNaturalNumber).invalidNel()
            assertThat(actual).isEqualTo(expected)
        }
    }
}
