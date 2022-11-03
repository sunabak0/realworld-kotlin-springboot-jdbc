package com.example.realworldkotlinspringbootjdbc.api_integration.helper

import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.time.api.constraints.OffsetRange
import org.assertj.core.api.Assertions.assertThat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class DatetimeVerificationHelperTest {
    class ExpectIso8601UtcAndParsable {
        @Property
        fun `正常系-ISO8601 かつ UTC の場合 true が戻り値`(
            @ForAll @OffsetRange(
                min = "Z",
                max = "Z"
            ) offsetDateTime: OffsetDateTime
        ) {
            /**
             * given:
             * - ISO8601 にフォーマット
             */
            val formatted = offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"))

            /**
             * when:
             */
            val actual = DatetimeVerificationHelper.expectIso8601UtcAndParsable(formatted)

            /**
             * then:
             */
            assertThat(actual).isTrue
        }

        @Property
        fun `正常系-ミリ秒なしの ISO8601 かつ UTC の場合 true が戻り値`(
            @ForAll @OffsetRange(
                min = "Z",
                max = "Z"
            ) offsetDateTime: OffsetDateTime
        ) {
            /**
             * given:
             * - ISO8601 にフォーマット
             */
            val formatted = offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"))

            /**
             * when:
             */
            val actual = DatetimeVerificationHelper.expectIso8601UtcWithoutMillisecondAndParsable(formatted)

            /**
             * then:
             */
            assertThat(actual).isTrue
        }
    }
}
