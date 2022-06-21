package com.example.realworldkotlinspringbootjdbc.sandbox.junit5

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream

/**
 * Table-Driven Test
 * Parameterized Test
 */
class PracticeParameterizedTest {
    class ParameterizedTestアノテーションを使ったテスト {
        @ParameterizedTest
        @ArgumentsSource(TestCase::class)
        fun multiArgsOfMethodSourceTest(id: Int, name: String, list: List<String>) {
            assertThat(1).isEqualTo(1)
            assertTrue(list.stream().allMatch({ s -> s.matches("^[A-Z]+$".toRegex()) }))
        }
        private class TestCase : ArgumentsProvider {
            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(
                    arguments(1, "hoge", listOf("A", "B")),
                    // arguments(2, "piyo", listOf("CC", "DD", "aa")),
                    arguments(3, "fuga", listOf("EEE", "FFF"))
                )
            }
        }
    }

    /**
     * TestFactoryアノテーション
     *
     * メリット
     * - タイトルが指定できる
     */
    class TestFactoryアノテーションを使ったテスト {
        @TestFactory
        fun testFactory(): List<DynamicNode> {
            return listOf(
                dynamicTest("Hoge") { assertThat(1).isEqualTo(1) },
                // dynamicTest("Fuga") { assert(false) },
                dynamicTest("Piyo") { assertThat(listOf("1", "2", "3")).allMatch { s -> s.matches("^[0-9]+$".toRegex()) } },
            )
        }
    }

    class TestFactoryアノテーションを使ったテスト2 {
        @TestFactory
        fun testFactory(): Stream<DynamicNode> {
            return Stream.of(
                dynamicTest("Hoge") { assertThat(1).isEqualTo(1) },
                // dynamicTest("Fuga") { assert(false) },
                dynamicTest("Piyo") { assertThat(listOf("1", "2", "3")).allMatch { s -> s.matches("^[0-9]+$".toRegex()) } },
            )
        }
    }

    class TestFactoryアノテーションを使ったテスト3 {
        @TestFactory
        fun testFactory(): Stream<DynamicNode> {
            return Stream.of(
                Pair("Hoge", "1"),
                // Pair("Fuga", "a"),
                Pair("Piyo", "2"),
            ).map { (title, target) -> dynamicTest(title) { assertThat(target).matches("^[0-9]+$") } }
        }
    }
}
