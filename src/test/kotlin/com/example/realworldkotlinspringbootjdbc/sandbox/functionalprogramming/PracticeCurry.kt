package com.example.realworldkotlinspringbootjdbc.sandbox.functionalprogramming

import arrow.core.curried
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PracticeCurry {
    val strong: (String, String, String) -> String = { id, style, body -> """<strong id="$id" style="$style">$body</strong>""" }
    @Test
    fun `引数が"a","b","c"の場合、id,style,bodyにそれぞれ"a","b","c"が入ったstrongタグが返る`() {
        val actual = strong("a", "b", "c")
        val expected = """<strong id="a" style="b">c</strong>"""
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `curry化`() {
        val curriedStrong: (id: String) -> (style: String) -> (body: String) -> String = { id -> { style -> { body -> strong(id, style, body) } } }
        val actual = curriedStrong("a")("b")("c")
        val expected = """<strong id="a" style="b">c</strong>"""
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Arrowを利用したcurry化`() {
        val curriedStrong: (id: String) -> (style: String) -> (body: String) -> String = strong.curried()
        val actual = curriedStrong("a")("b")("c")
        val expected = """<strong id="a" style="b">c</strong>"""
        assertThat(actual).isEqualTo(expected)
    }
}
