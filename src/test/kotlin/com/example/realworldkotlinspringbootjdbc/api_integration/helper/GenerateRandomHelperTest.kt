package com.example.realworldkotlinspringbootjdbc.api_integration.helper

import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.IntRange
import org.assertj.core.api.Assertions.assertThat

class GenerateRandomHelperTest {
    class GetRandomString {
        @Property
        fun `正常系-生成された文字列が英大文字小文字数字`(
            @ForAll @IntRange(min = 0, max = 100) randomInt: Int
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = GenerateRandomHelper.getRandomString(randomInt)

            /**
             * then:
             */
            val expectedPattern = "^[a-zA-Z0-9]{$randomInt}$"
            assertThat(actual).matches(expectedPattern)
        }
    }
}
