package com.example.realworldkotlinspringbootjdbc.domain.article

import arrow.core.invalidNel
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CommentIdTest {
    class NewTest {
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
    }
}
