package com.example.realworldkotlinspringbootjdbc.sandbox.functionalprogramming

import arrow.core.andThen
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PracticeFunctionComposition {
    val sum: (Int, Int) -> Int = { a, b -> a + b }
    val twice: (Int) -> Int = { a -> a * 2 }
    val strong: (Int) -> String = { a -> "<strong>$a</strong>" }
    @Test
    fun `関数合成`() {
        val sumAndThenTwiceAndStrong: (Int, Int) -> String = { a, b -> strong(twice(sum(a, b))) }
        val actual = sumAndThenTwiceAndStrong(1, 2)
        val expected = "<strong>6</strong>"
        assertThat(actual).isEqualTo(expected)
    }

    /**
     * aFunc: (Int, Int) -> Int
     * bFunc: (Int) -> Int
     * aFunc と bFuncの関数合成結果xFunc: (Int, Int) -> Int
     *
     * cFunc: (Int) -> String
     * xFuncとcFuncの関数合成結果yFunc: (Int, Int) -> String
     *
     * aFuncとbFuncとcFuncの関数合成結果: (Int, Int) -> String
     */
    @Test
    fun `関数合成をするcompose関数`() {
        val x: ((Int, Int) -> Int, (Int) -> Int) -> (Int, Int) -> Int = {
                aFunc, bFunc ->
            { x, y -> bFunc(aFunc(x, y)) }
        }
        val y: ((Int, Int) -> Int, (Int) -> String) -> (Int, Int) -> String = {
                aFunc, bFunc ->
            { x, y -> bFunc(aFunc(x, y)) }
        }
        val sumAndThenTwiceAndStrong = y(x(sum, twice), strong)
        val actual = sumAndThenTwiceAndStrong(1, 2)
        val expected = "<strong>6</strong>"
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Arrowを利用した場合の関数合成`() {
        val sumAndThenTwiceAndStrong = sum andThen twice andThen strong
        val actual = sumAndThenTwiceAndStrong(1, 2)
        val expected = "<strong>6</strong>"
        assertThat(actual).isEqualTo(expected)
    }
}
