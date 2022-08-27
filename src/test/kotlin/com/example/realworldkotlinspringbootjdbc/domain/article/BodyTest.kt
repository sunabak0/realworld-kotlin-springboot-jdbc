package com.example.realworldkotlinspringbootjdbc.domain.article

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ArbitrarySupplier

class BodyTest {
    /**
     * Bodyの有効な範囲のStringプロパティ
     */
    class BodyValidRange : ArbitrarySupplier<String> {
        override fun get(): Arbitrary<String> =
            Arbitraries.strings()
                .filter { !it.startsWith("diff-") }
    }
}
