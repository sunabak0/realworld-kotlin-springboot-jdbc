package com.example.realworldkotlinspringbootjdbc.sandbox.arrow

import arrow.core.Tuple4
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PracticeTuple {
    @Test
    fun `TupleXは属性がそれぞれ等価であれば、等価である`() {
        val a = Tuple4(1, 2, 3, 4)
        val b = Tuple4(1, 2, 3, 4)
        assertThat(a).isEqualTo(b)
    }
}
