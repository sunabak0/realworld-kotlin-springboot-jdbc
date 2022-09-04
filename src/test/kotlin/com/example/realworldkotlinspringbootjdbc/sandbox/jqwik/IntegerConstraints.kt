package com.example.realworldkotlinspringbootjdbc.sandbox.jqwik

import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.IntRange
import net.jqwik.api.constraints.Negative
import net.jqwik.api.constraints.Positive
import org.assertj.core.api.Assertions.assertThat

/**
 * jqwik の Integer Constraints の境界値（0未満、0、0超過）をテストする
 * [integer-constrains](https://jqwik.net/docs/current/user-guide.html#integer-constraints)
 */
class IntegerConstraints {
    class BoundaryTest {
        @Property
        fun `@Negative のとき負の整数`(
            @ForAll @Negative negativeNumber: Int
        ) {
            /**
             * given:
             */

            /**
             * when:
             */

            /**
             * then:
             */
            assertThat(negativeNumber < 0).isTrue
        }

        @Property
        fun `@Positive のとき正の整数`(
            @ForAll @Positive positiveNumber: Int
        ) {
            /**
             * given:
             */

            /**
             * when:
             */

            /**
             * then:
             */
            assertThat(positiveNumber > 0).isTrue
        }

        @Property
        fun `@IntRange を用いると、0 を指定できる`(
            @ForAll @IntRange(min = 0, max = 0) zero: Int
        ) {
            /**
             * given:
             */

            /**
             * when:
             */

            /**
             * then:
             */
            assertThat(zero == 0).isTrue
        }
    }
}
