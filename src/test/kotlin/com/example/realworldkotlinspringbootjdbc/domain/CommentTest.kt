package com.example.realworldkotlinspringbootjdbc.domain

import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.text.SimpleDateFormat
import java.util.stream.Stream
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body as CommentBody

class CommentTest {
    data class TestCase(
        val title: String,
        val comment: Comment,
        val otherComment: Comment,
        val expected: Boolean
    )

    @TestFactory
    fun commentEqualsTest(): Stream<DynamicNode> {
        val date1 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
        val date2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-02T00:00:00+09:00")
        return Stream.of(
            TestCase(
                "CommentId が一致する場合、他のプロパティが異なっていても、true を返す（コメントは更新されても、Idが同じなら同一のコメントとして判断される）",
                Comment.newWithoutValidation(
                    CommentId.newWithoutValidation(1),
                    CommentBody.newWithoutValidation("dummy-body-1"),
                    createdAt = date1,
                    updatedAt = date1,
                    UserId(1),
                ),
                Comment.newWithoutValidation(
                    CommentId.newWithoutValidation(1),
                    CommentBody.newWithoutValidation("dummy-body-2"),
                    createdAt = date1,
                    updatedAt = date2,
                    UserId(1)
                ),
                true
            ),
            TestCase(
                "CommentId が一致しない場合、他のプロパティが全て同じでも、true を返す（コメントが同じタイミングと内容で作成されても、Id が異なれば違うコメントとして判断される）",
                Comment.newWithoutValidation(
                    CommentId.newWithoutValidation(1),
                    CommentBody.newWithoutValidation("dummy-body-1"),
                    createdAt = date1,
                    updatedAt = date1,
                    UserId(1)
                ),
                Comment.newWithoutValidation(
                    CommentId.newWithoutValidation(2),
                    CommentBody.newWithoutValidation("dummy-body-1"),
                    createdAt = date1,
                    updatedAt = date1,
                    UserId(1)
                ),
                false
            )
        ).map { testCase ->
            dynamicTest(testCase.title) {
                assertThat(testCase.comment == testCase.otherComment).isEqualTo(testCase.expected)
            }
        }
    }
}
