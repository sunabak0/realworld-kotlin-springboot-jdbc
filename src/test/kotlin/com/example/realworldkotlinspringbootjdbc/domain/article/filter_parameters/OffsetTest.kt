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

class OffsetTest {
    @Nested
    @DisplayName("FactoryMethod-new")
    class NewTest {
        @Test
        fun `正常系-引数がnullだとデフォルト値として生成される`() {
            /**
             * given:
             */
            val offsetArg = null

            /**
             * when:
             */
            val actual = Offset.new(offsetArg)

            /**
             * then:
             */
            val defaultValue = 0
            when (actual) {
                is Invalid -> assert(false)
                is Valid -> assertThat(actual.value.value).isEqualTo(defaultValue)
            }
        }

        @Property(tries = 100)
        fun `準正常系-引数はIntegerに変換できる必要がある`(
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
            val intMaxOverActual = Offset.new(intMaxOverString)
            val intMinUnderActual = Offset.new(intMinUnderString)

            /**
             * then:
             * - 変換に失敗する旨のエラー
             */
            val intMaxOverExpected = Offset.ValidationError.FailedConvertToInteger(intMaxOverString).invalidNel()
            val intMinUnderExpected = Offset.ValidationError.FailedConvertToInteger(intMinUnderString).invalidNel()
            assertThat(intMaxOverActual).isEqualTo(intMaxOverExpected)
            assertThat(intMinUnderActual).isEqualTo(intMinUnderExpected)
        }

        @Property(tries = 100)
        fun `正常系&準正常系-引数は0〜Intの最大値である必要がある`(
            @ForAll @IntRange(min = 0, max = Int.MAX_VALUE) valid: Int,
            @ForAll @IntRange(min = Int.MIN_VALUE, max = -1) minimumUnder: Int
        ) {
            /**
             * given:
             * - Offsetの範囲内の数字
             * - Offsetの下限値未満の数字
             */
            val validString = valid.toString()
            val minimumUnderString = minimumUnder.toString()

            /**
             * when:
             */
            val validActual = Offset.new(validString)
            val minimumUnderActual = Offset.new(minimumUnderString)

            /**
             * then:
             * - 中身参照して与えられた数値
             * - 下限値以上である必要がある旨のエラー
             */
            val requireMinimumOrOver = Offset.ValidationError.RequireMinimumOrOver(minimumUnder).invalidNel()
            when (validActual) {
                is Invalid -> assert(false)
                is Valid -> assertThat(validActual.value.value).isEqualTo(valid)
            }
            assertThat(minimumUnderActual).isEqualTo(requireMinimumOrOver)
        }
    }
}
