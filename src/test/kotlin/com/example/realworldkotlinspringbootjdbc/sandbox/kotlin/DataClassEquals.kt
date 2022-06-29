package com.example.realworldkotlinspringbootjdbc.sandbox.kotlin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.util.Date

/**
 * data class の比較
 */
class DataClassEquals {
    @Nested
    class SimpleDataClass {
        data class Person (
            val name: String,
            val age: Int,
            val birthday: Date,
        )
        @Test
        fun `data classはプロパティがそれぞれ同じ値の場合、等価である`() {
            val a = Person("Foo", 20, SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"))
            val b = Person("Foo", 20, SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"))
            assertThat(a == b).isTrue
        }
        @Test
        fun `data classはプロパティが1つでも異なる場合、不等価である`() {
            val a = Person("Foo", 20, SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"))
            val b = Person("Foo", 99, SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"))
            assertThat(a == b).isFalse
        }
    }

    @Nested
    class NestedDataClass {
        data class Parent(
            val name: String,
            val child: Child,
        )
        data class Child(
            val name: String,
        )
        @Test
        fun `data classは入れ子でも比較可能である`() {
            val a = Parent("Foo", Child("Bar"))
            val b = Parent("Foo", Child("Bar"))
            assertThat(a == b).isTrue
        }
    }

    @Nested
    class OverridedEqualsDataClass {
        /**
         * 識別子: StudentNumber
         */
        data class Student(
            val studentNumber: StudentNumber,
            val name: String,
            val club: String,
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as Student
                return this.studentNumber == other.studentNumber
            }
            override fun hashCode(): Int {
                return studentNumber.hashCode()
            }
        }
        data class StudentNumber(val value: Int)

        @Test
        fun `部活が異なっても、識別子が一致していれば等価である`() {
            val a = Student(StudentNumber(5), "田中太郎", "野球部")
            val b = Student(StudentNumber(5), "田中太郎", "テニス部")
            assertThat(a == b).isTrue
        }
        @Test
        fun `識別子が異なる場合、他が一致していても不等価である`() {
            val a = Student(StudentNumber(1), "田中太郎", "野球部")
            val b = Student(StudentNumber(2), "田中太郎", "野球部部")
            assertThat(a == b).isFalse
        }
    }
}