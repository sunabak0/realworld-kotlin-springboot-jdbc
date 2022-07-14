package com.example.realworldkotlinspringbootjdbc.domain

import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.text.SimpleDateFormat
import java.util.stream.Stream
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body as CommentBody

class CommentTest {
    data class TestCase(
        val title: String,
        val comment: Comment,
        val otherComment: Comment,
        val expect: Boolean
    )

    @TestFactory
    fun commentEqualsTest(): Stream<DynamicNode> {
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
        return Stream.of(
            TestCase(
                "CommentId が一致してい場合、同値である",
                Comment.newWithoutValidation(
                    CommentId.newWithoutValidation(1),
                    CommentBody.newWithoutValidation("dummy-body-1"),
                    createdAt = date,
                    updatedAt = date,
                    UserId(1),
                ),
                Comment.newWithoutValidation(
                    CommentId.newWithoutValidation(1),
                    CommentBody.newWithoutValidation("dummy-body-2"),
                    createdAt = date,
                    updatedAt = date,
                    UserId(1)
                ),
                true
            )
        ).map { testCase ->
            dynamicTest(testCase.title) {
                assertThat(testCase.comment == testCase.otherComment).isEqualTo(testCase.expect)
            }
        }
    }

    @Test
    fun `Comment は識別子 CommentId が一致していない場合、他が同じでも異なる値であることを期待する`() {
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
        val comment1 = Comment.newWithoutValidation(
            CommentId.newWithoutValidation(1),
            CommentBody.newWithoutValidation("dummy-body-1"),
            createdAt = date,
            updatedAt = date,
            UserId(1)
        )
        val comment2 = Comment.newWithoutValidation(
            CommentId.newWithoutValidation(2),
            CommentBody.newWithoutValidation("dummy-body-1"),
            createdAt = date,
            updatedAt = date,
            UserId(1)
        )
        val actual = comment1 != comment2
        assertThat(actual).isTrue
    }
}
