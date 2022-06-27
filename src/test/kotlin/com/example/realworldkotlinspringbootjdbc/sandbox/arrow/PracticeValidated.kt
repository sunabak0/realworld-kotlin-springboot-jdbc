package com.example.realworldkotlinspringbootjdbc.sandbox.arrow

import arrow.core.valid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PracticeValidated {
    @Test
    fun `Validのmapの戻り値は、ValidにWrapされる`() {
        val actual = "right".valid().map { "$it-foo" }
        val expected = "right-foo".valid()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Validのfoldの戻り値は、ValidにWrapされずにそのままである`() {
        val actual = "right".valid().fold(
            { it },
            { "$it-foo" }
        )
        val expected = "right-foo"
        assertThat(actual).isEqualTo(expected)
    }
}
