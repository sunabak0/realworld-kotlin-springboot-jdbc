package com.example.realworldkotlinspringbootjdbc.sandbox.arrow

import arrow.core.Either
import arrow.core.nonEmptyListOf
import arrow.core.traverse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class traverse素振り {
    object DummyError
    @Test
    fun 全てRightだった時まとめてEither1つにできる() {
        fun double(v : Int): Either<DummyError, Int> = Either.Right(v * 2)
        val actual = nonEmptyListOf(1, 2, 3).traverse { double(it) }
        val expected = Either.Right(nonEmptyListOf(2, 4, 6))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun LeftとRightが混じった時は早期リターンっぽい() {
        val list = mutableListOf<Int>()
        fun hello(v : Int): Either<String, Boolean> {
            list.add(v)
            return when (v % 3) {
                0 -> { Either.Right(true) }
                1 -> { Either.Left("NG-1") }
                else -> { Either.Left("NG-2") }
            }
        }
        val actual = nonEmptyListOf(0, 1, 2, 3, 4, 5, 6).traverse { hello(it) }
        val expected = Either.Left("NG-1")
        assertThat(actual).isEqualTo(expected)
        assertThat(list).isEqualTo(listOf(0, 1))
    }
}