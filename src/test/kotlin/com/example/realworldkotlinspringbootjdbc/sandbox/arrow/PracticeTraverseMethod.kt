package com.example.realworldkotlinspringbootjdbc.sandbox.arrow

import arrow.core.Either
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import arrow.core.traverse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PracticeTraverseMethod {
    object DummyError
    @Test
    fun 全てRightだった時まとめてEither1つにできる() {
        fun double(v: Int): Either<DummyError, Int> = (v * 2).right()
        val actual = nonEmptyListOf(1, 2, 3).traverse { double(it) }
        val expected = nonEmptyListOf(2, 4, 6).right()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun LeftとRightが混じった時は早期リターンっぽい() {
        val list = mutableListOf<Int>()
        fun hello(v: Int): Either<String, Boolean> {
            list.add(v)
            return when (v % 3) {
                0 -> { true.right() }
                1 -> { "NG-1".left() }
                else -> { "NG-2".left() }
            }
        }
        val actual = nonEmptyListOf(0, 1, 2, 3, 4, 5, 6).traverse { hello(it) }
        val expected = ("NG-1").left()
        assertThat(actual).isEqualTo(expected)
        assertThat(list).isEqualTo(listOf(0, 1))
    }
}
