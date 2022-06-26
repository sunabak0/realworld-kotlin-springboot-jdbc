package com.example.realworldkotlinspringbootjdbc.sandbox.arrow

import arrow.core.right
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PracticeEither {
    @Test
    fun `Rightのmapの戻り値は、RightにWrapされる`() {
        val actual = "right".right().map { "$it-foo" }
        val expected = "right-foo".right()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Rightのfoldの戻り値は、RightにWrapされずにそのままである`() {
        val actual = "right".right().fold(
            { it },
            { "$it-foo" }
        )
        val expected = "right-foo"
        assertThat(actual).isEqualTo(expected)
    }
}
