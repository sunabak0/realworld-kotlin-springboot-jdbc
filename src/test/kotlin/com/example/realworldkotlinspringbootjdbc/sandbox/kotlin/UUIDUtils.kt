package com.example.realworldkotlinspringbootjdbc.sandbox.kotlin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class UUIDUtils {
    class Generate {
        @Test
        fun `UUID を生成したとき、ハイフンを除くと 32 文字である`() {
            /**
             * given:
             */

            /**
             * when:
             */
            val uuid = UUID.randomUUID().toString().split("-").joinToString("")
            val uuidLength = uuid.length

            /**
             * then:
             * - 32 文字であることを確認する
             * - 文字列自体はランダムのため検証しない
             */
            val expected = 32
            assertThat(uuidLength).isEqualTo(expected)
        }
    }
}
