package com.example.realworldkotlinspringbootjdbc.domain.article.filter_parameters

import arrow.core.Invalid
import arrow.core.Valid
import arrow.core.invalidNel
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.IntRange
import net.jqwik.api.constraints.LongRange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LimitTest {
    @Nested
    @DisplayName("FactoryMethod-new")
    class NewTest {
        @Test
        fun `引数がnullだとデフォルト値として生成される`() {
            /**
             * given:
             */
            val limitArg = null

            /**
             * when:
             */
            val actual = Limit.new(limitArg)

            /**
             * then:
             */
            val defaultValue = 20
            when (actual) {
                is Invalid -> assert(false)
                is Valid -> assertThat(actual.value.value).isEqualTo(defaultValue)
            }
        }

        @Property(tries = 100)
        fun `引数はIntegerに変換できる必要がある`(
            @ForAll @LongRange(min = Int.MAX_VALUE.toLong() + 1L) intMaxOver: Long,
            @ForAll @LongRange(min = Long.MIN_VALUE, max = Int.MIN_VALUE.toLong() - 1L) intMinUnder: Long,
        ) {
            /**
             * given:
             * - Intの上限を超過する数字
             * - Intの下限を未満の数字
             */
            val intMaxOverString = intMaxOver.toString()
            val intMinUnderString = intMinUnder.toString()

            /**
             * when:
             */
            val intMaxOverActual = Limit.new(intMaxOverString)
            val intMinUnderActual = Limit.new(intMinUnderString)

            /**
             * then:
             * - 変換に失敗する旨のエラー
             */
            val intMaxOverExpected = Limit.ValidationError.FailedConvertToInteger(intMaxOverString).invalidNel()
            val intMinUnderExpected = Limit.ValidationError.FailedConvertToInteger(intMinUnderString).invalidNel()
            assertThat(intMaxOverActual).isEqualTo(intMaxOverExpected)
            assertThat(intMinUnderActual).isEqualTo(intMinUnderExpected)
        }

        @Property(tries = 100)
        fun `引数は1〜100である必要がある`(
            @ForAll @IntRange(min = 1, max = 100) valid: Int,
            @ForAll @IntRange(min = 101) maximumOver: Int,
            @ForAll @IntRange(max = 0) minimumUnder: Int
        ) {
            /**
             * given:
             * - Limitの範囲内の数字
             * - Limitの上限値超過の数字
             * - Limitの下限値未満の数字
             */
            val validString = valid.toString()
            val maximumOverString = maximumOver.toString()
            val minimumUnderString = minimumUnder.toString()

            /**
             * when:
             */
            val validActual = Limit.new(validString)
            val maximumOverActual = Limit.new(maximumOverString)
            val minimumUnderActual = Limit.new(minimumUnderString)

            /**
             * then:
             * - 中身参照して与えられた数値
             * - 上限値以下である必要がある旨のエラー
             * - 下限値以上である必要がある旨のエラー
             */
            val requireMaximumOrUnder = Limit.ValidationError.RequireMaximumOrUnder(maximumOver).invalidNel()
            val requireMinimumOrOver = Limit.ValidationError.RequireMinimumOrOver(minimumUnder).invalidNel()
            when (validActual) {
                is Invalid -> assert(false)
                is Valid -> assertThat(validActual.value.value).isEqualTo(valid)
            }
            assertThat(maximumOverActual).isEqualTo(requireMaximumOrUnder)
            assertThat(minimumUnderActual).isEqualTo(requireMinimumOrOver)
        }
    }
}
