package com.example.realworldkotlinspringbootjdbc.sandbox.arrow

import arrow.core.nonEmptyListOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

//
// NonEmptyList
//
// https://arrow-kt.io/docs/apidocs/arrow-core/arrow.core/-non-empty-list/
//
@Suppress("NonAsciiCharacters")
class OfficialNonEmptyListSample {
    @Test
    fun NonEmptyListの要素数が1以上である() {
        // val value = nonEmptyListOf() // コンパイルが通らない
        val value = nonEmptyListOf(1,2,3,4,5)
        assertThat(value).isNotEmpty
    }

    @Test
    fun foldLeftは畳み込み() {
        val actual = nonEmptyListOf(1, 1, 1, 1).foldLeft(0) { acc, n -> acc + n }
        val expected = 4
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun mapはそれぞれの要素に対して適用() {
        val actual = nonEmptyListOf(1, 1, 1, 1).map { it + 1 }
        val expected = nonEmptyListOf(2, 2, 2, 2)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun flatMapは複数のNonEmptyListを結合する() {
        val a = nonEmptyListOf(1, 2, 3)
        val b = nonEmptyListOf(4, 5)
        val actual = a.flatMap { alpha ->
            b.map { beta ->
                alpha + beta
                // 1周目: nonEmptyListOf(1+4, 1+5)
                // 2周目: nonEmptyListOf(2+4, 2+5)
                // 3周目: nonEmptyListOf(3+4, 3+5)
            }
        }
        assertThat(actual).isEqualTo(nonEmptyListOf(5, 6, 6, 7, 7, 8))
    }


    @Test
    fun zipは複数のNonEmptyListを並列にまとめ直す() {
        // 人
        data class Person(val id: UUID, val name: String, val year: Int)
        // 人が持つそれぞれの型のList(長さは全て2)
        val ids = nonEmptyListOf(UUID.randomUUID(), UUID.randomUUID())
        val names = nonEmptyListOf("William Alvin Howard", "Haskell Curry")
        val years = nonEmptyListOf(1926, 1900)
        val actual = ids.zip(names, years) { id, name, year -> Person(id, name, year) }
        val expected = nonEmptyListOf(
            Person(ids[0], names[0], years[0]),
            Person(ids[1], names[1], years[1]),
        )
        assertThat(actual).isEqualTo(expected)
    }
}

// typealias Nel<A> = NonEmptyList<A>
// typealias ValidatedNel<E, A> = Validated<Nel<E>, A>
