package com.example.realworldkotlinspringbootjdbc.api_integration.helper

import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.IntRange
import org.assertj.core.api.Assertions.assertThat

class GenerateRandomHelperTest {
    class GetRandomString {
        @Property
        fun `正常系-指定した長さの文字種別が英大文字小文字数字である文字列が生成される`(
            @ForAll @IntRange(min = 0, max = 100) length: Int
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = GenerateRandomHelper.getRandomString(length)

            /**
             * then:
             */
            val expectedPattern = "^[a-zA-Z0-9]{$length}$"
            assertThat(actual).matches(expectedPattern)
        }
    }
}
