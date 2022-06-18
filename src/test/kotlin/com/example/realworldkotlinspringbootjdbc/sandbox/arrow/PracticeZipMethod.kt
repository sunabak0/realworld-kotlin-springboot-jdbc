package com.example.realworldkotlinspringbootjdbc.sandbox.arrow

import arrow.core.Either
import arrow.core.Validated
import arrow.core.ValidatedNel
import arrow.core.handleErrorWith
import arrow.core.invalid
import arrow.core.invalidNel
import arrow.core.nonEmptyListOf
import arrow.core.validNel
import arrow.core.zip
import arrow.typeclasses.Semigroup
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

//
// https://arrow-kt.io/docs/apidocs/arrow-core/arrow.core/zip.html
//
class PracticeZipMethod {

    // fun <K, A, B> Map<K, A>.zip(other: Map<K, B>): Map<K, Pair<A, B»
    @Test
    fun _2つのMapに対してzipすると重複するKeyのPairのMapが返る() {
        val a = mapOf(
            1 to "a-A", // 重複無し
            2 to "a-B", // 重複
            3 to "a-C", // 重複
        )
        val b = mapOf(
            2 to "b-B", // 重複
            3 to "b-C", // 重複
            4 to "b-D", // 重複無し
        )
        val actual = a.zip(b)
        val expected = mapOf(2 to Pair("a-B", "b-B"), 3 to Pair("a-C", "b-C"))
        assertThat(actual).isEqualTo(expected)
    }

    // inline fun <Key, A, B, C> Map<Key, A>.zip(other: Map<Key, B>, map: (Key, A, B) -> C): Map<Key, C>
    @Test
    fun Blockを渡すとPairを別の型に変換できる() {
        val a = mapOf(
            1 to "a-A", // 重複無し
            2 to "a-B", // 重複
            3 to "a-C", // 重複
        )
        val b = mapOf(
            2 to "b-B", // 重複
            3 to "b-C", // 重複
            4 to "b-D", // 重複無し
        )
        val actual = a.zip(b) { _, av, bv -> "$av,$bv" }
        val expected = mapOf(2 to "a-B,b-B", 3 to "a-C,b-C")
        assertThat(actual).isEqualTo(expected)
    }

    // fun <A, B, C> Either<A, B>.zip(fb: Either<A, C>): Either<A, Pair<B, C»
    @Test
    fun _2つのRightに対してzipすると1つのPathになってRightが返る() {
        fun a(): Either<String, Boolean> = Either.Right(true)
        fun b(): Either<String, Long> = Either.Right(1L)
        // zipの結果 => Either<String, Pair<Boolean, Long>>

        val actual = a().zip(b())
        val expected = Either.Right(Pair(true, 1L))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun _3つのRightに対してzipするとBlockありきで別の型に変換できる() {
        data class Dummy(val x: Boolean, val y: Long, val z: String)
        fun a(): Either<String, Boolean> = Either.Right(true)
        fun b(): Either<String, Long> = Either.Right(1L)
        fun c(): Either<String, String> = Either.Right("C-Right")

        val actual = a().zip(b(), c()) { av, bv, cv -> Dummy(av, bv, cv) }
        val expected = Either.Right(Dummy(true, 1L, "C-Right"))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun _3つのEitherに対して2つのLeftが混ざっていると1つ目だけがhandleWithに行く() {
        val list = mutableListOf<String>()
        data class Dummy(val x: Boolean, val y: Long, val z: String)
        fun a(): Either<String, Boolean> = Either.Right(true)
        fun b(): Either<String, Long> { list.add("B"); return Either.Left("B-Left") }
        fun c(): Either<String, String> { list.add("C"); return Either.Left("C-Left") }

        val actual = a().zip(b(), c()) { av, bv, cv -> Dummy(av, bv, cv) }
            .handleErrorWith { Either.Left(it) }
        val expected = Either.Left("B-Left") // C-Leftがない
        assertThat(actual).isEqualTo(expected)
        assertThat(list).isEqualTo(listOf("B", "C")) // c()は実行されている
    }

    // Validationに使える!!
    @Test
    fun SemigroupのNelを使えばhandleWithで一気に扱える() {
        data class Dummy(val x: Boolean, val y: Long, val z: String)
        fun a(): ValidatedNel<String, Boolean> = true.validNel()
        fun b(): ValidatedNel<String, Long> = "B-Left".invalidNel()
        fun c(): ValidatedNel<String, String> = "C-Left".invalidNel()

        val actual = a().zip(Semigroup.nonEmptyList(), b(), c()) { av, bv, cv -> Dummy(av, bv, cv) }
            .handleErrorWith { it.invalid() }
        val expected = Validated.Invalid(nonEmptyListOf("B-Left", "C-Left"))
        assertThat(actual).isEqualTo(expected)
    }
}
