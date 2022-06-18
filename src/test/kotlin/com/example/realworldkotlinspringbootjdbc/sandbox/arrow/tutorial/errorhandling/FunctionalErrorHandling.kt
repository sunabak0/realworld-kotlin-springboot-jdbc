package com.example.realworldkotlinspringbootjdbc.sandbox.arrow.tutorial.errorhandling

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Nel
import arrow.core.ValidatedNel
import arrow.core.continuations.either
import arrow.core.continuations.nullable
import arrow.core.handleErrorWith
import arrow.core.invalidNel
import arrow.core.nonEmptyListOf
import arrow.core.traverse
import arrow.core.validNel
import arrow.core.zip
import arrow.typeclasses.Semigroup
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FunctionalErrorHandling {
    object Lettuce
    object Knife
    object Salad

    //
    // Case 01
    // 例外ってつらいよね 編
    //
    class Case01 {
        //
        // 実装する必要がある関数群
        //
        private interface Requirements {
            fun takeFoodFromRefrigerator(): Lettuce
            fun getKnife(): Knife
            fun prepare(tool: Knife, ingredient: Lettuce): Salad
        }

        //
        // 関数シグネチャから例外が投げられることはワカラナイ => 隠せる
        //
        class RequirementsImpl : Requirements {
            override fun takeFoodFromRefrigerator(): Lettuce = throw RuntimeException("お店で材料を買ってきてください")
            override fun getKnife(): Knife = throw RuntimeException("ナイフを研いでください")
            override fun prepare(tool: Knife, ingredient: Lettuce): Salad = Salad
        }
    }

    //
    // Case 02
    // Nullable の利用
    //
    class Case02 {
        //
        // 返り値がNullableになった場合
        //
        private interface Requirements {
            fun takeFoodFromRefrigerator(): Lettuce?
            fun getKnife(): Knife?
            fun prepare(tool: Knife, ingredient: Lettuce): Salad?
        }

        //
        // 実装は以下のように書ける
        //
        class RequirementsImpl : Requirements {
            override fun takeFoodFromRefrigerator(): Lettuce? = null
            override fun getKnife(): Knife? = null
            override fun prepare(tool: Knife, ingredient: Lettuce): Salad? = Salad
        }

        //
        // この時の実装ってletを使うじゃない？
        //
        fun prepareLunch(): Salad? {
            val impl = RequirementsImpl()
            val lettuce = impl.takeFoodFromRefrigerator()
            val knife = impl.getKnife()
            return knife?.let { k -> lettuce?.let { l -> impl.prepare(k, l) } }
        }

        //
        // Arrow Coreにある nullableブロックを使って書ける
        // (suspend はまだ謎)
        //
        suspend fun prepareLunch2(): Salad? =
            nullable {
                val impl = RequirementsImpl()
                val lettuce = impl.takeFoodFromRefrigerator().bind()
                val knife = impl.getKnife().bind()
                val salad = impl.prepare(knife, lettuce).bind()
                salad
            }
    }

    //
    // Case 03
    // Either の利用
    //
    class Case03 {
        //
        // エラーをsealed classで定義する
        // ついでにtypealiasを宣言(宣言はtopレベルじゃないと怒られたので、topレベルに記述(一番下に書いてある))
        //
        sealed class CookingException {
            object NastyLettuce : CookingException() // レタスが汚い
            object KnifeIsDull : CookingException() // ナイフの切れ味が悪い
            data class InsufficientAmountOfLettuce(val quantityInGrams: Int) : CookingException() // レタスの量が足りない
        }

        //
        // 返り値が Either
        //
        private interface Requirements {
            fun takeFoodFromRefrigerator(): Either<NastyLettuce, Lettuce>
            fun getKnife(): Either<KnifeIsDull, Knife>
            fun lunch(tool: Knife, ingredient: Lettuce): Either<InsufficientAmountOfLettuce, Salad>
        }

        //
        // こんな実装になる
        //
        class RequirementsImpl : Requirements {
            override fun takeFoodFromRefrigerator(): Either<NastyLettuce, Lettuce> = Right(Lettuce)
            override fun getKnife(): Either<KnifeIsDull, Knife> = Right(Knife)
            override fun lunch(tool: Knife, ingredient: Lettuce): Either<InsufficientAmountOfLettuce, Salad> = Left(
                InsufficientAmountOfLettuce(5)
            )
        }

        //
        // either ブロックを使って書ける
        // (suspend はまだ謎)
        //
        suspend fun prepareEither(): Either<CookingException, Salad> =
            either {
                val impl = RequirementsImpl()
                val lettuce = impl.takeFoodFromRefrigerator().bind()
                val knife = impl.getKnife().bind()
                val salad = impl.lunch(knife, lettuce).bind()
                salad
            }
    }
}
typealias NastyLettuce = FunctionalErrorHandling.Case03.CookingException.NastyLettuce
typealias KnifeIsDull = FunctionalErrorHandling.Case03.CookingException.KnifeIsDull
typealias InsufficientAmountOfLettuce = FunctionalErrorHandling.Case03.CookingException.InsufficientAmountOfLettuce

//
// https://arrow-kt.io/docs/patterns/error_handling/#alternative-validation-strategies--failing-fast-vs-accumulating-errors
// バリデーション戦略
//
class AlternativeValidationStrategy {
    sealed class ValidationError(val msg: String) {
        data class DoesNotContain(val value: String) : ValidationError("${value}が含まれていません")
        data class MaxLength(val value: Int) : ValidationError("${value}文字は長すぎます")
        data class NotAnEmail(val reasons: Nel<ValidationError>) : ValidationError("メールアドレスとして不正です")
    }

    data class FormField(val label: String, val value: String) // フォームの項目
    data class Email(val value: String) // Email

    sealed class Strategy {
        object FailFast : Strategy()
        object ErrorAccumulation : Strategy()
    }

    object Rules {
        // 特定の文字列が含まれているか
        private fun FormField.contains(needle: String): ValidatedNel<ValidationError, FormField> =
            if (value.contains(needle, false)) validNel()
            else ValidationError.DoesNotContain(needle).invalidNel()
        // 最大長
        private fun FormField.maxLength(maxLength: Int): ValidatedNel<ValidationError, FormField> =
            if (value.length <= maxLength) validNel()
            else ValidationError.MaxLength(maxLength).invalidNel()
        //
        // エラーの累積
        //
        private fun FormField.validateErrorAccumulate(): ValidatedNel<ValidationError, Email> =
            contains("@").zip(
                Semigroup.nonEmptyList(), // accumulates errors in a non empty list, can be omited for NonEmptyList
                maxLength(250)
            ) { _, _ -> Email(value) }.handleErrorWith { ValidationError.NotAnEmail(it).invalidNel() }
        //
        // エラーの早期リターン
        //
        private fun FormField.validateFailFast(): Either<Nel<ValidationError>, Email> =
            either.eager {
                contains("@").bind() // fails fast on first error found
                maxLength(250).bind()
                Email(value)
            }

        operator fun invoke(strategy: Strategy, fields: List<FormField>): Either<Nel<ValidationError>, List<Email>> =
            when (strategy) {
                Strategy.FailFast ->
                    // fields.traverseEither { it.validateFailFast() } // 非推奨っぽかった
                    fields.traverse { it.validateFailFast() }
                Strategy.ErrorAccumulation ->
                    // fields.traverseValidated(Semigroup.nonEmptyList()) { // 非推奨っぽかった
                    fields.traverse(Semigroup.nonEmptyList()) {
                        it.validateErrorAccumulate()
                    }.toEither()
            }
    }

    @Test
    fun `エラーの累積`() {
        val fields = listOf(
            FormField("Invalid Email Domain Label", "nowhere.com"),
            FormField("Too Long Email Label", "nowheretoolong${(0..251).map { "g" }}"), // this fails
            FormField("Valid Email Label", "getlost@nowhere.com")
        )
        val actual = Rules(Strategy.ErrorAccumulation, fields)
        val expected = Left(
            nonEmptyListOf(
                ValidationError.NotAnEmail(
                    reasons = nonEmptyListOf(ValidationError.DoesNotContain(value = "@"))
                ),
                ValidationError.NotAnEmail(
                    reasons = nonEmptyListOf(
                        ValidationError.DoesNotContain(value = "@"),
                        ValidationError.MaxLength(value = 250)
                    )
                )
            )
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `エラーの早期リターン`() {
        val fields = listOf(
            FormField("Invalid Email Domain Label", "nowhere.com"),
            FormField("Too Long Email Label", "nowheretoolong${(0..251).map { "g" }}"), // this fails
            FormField("Valid Email Label", "getlost@nowhere.com")
        )
        val actual = Rules(Strategy.FailFast, fields)
        val expected = Left(nonEmptyListOf(ValidationError.DoesNotContain(value = "@")))
        assertThat(actual).isEqualTo(expected)
    }
}
