package com.example.realworldkotlinspringbootjdbc.domain

import com.example.realworldkotlinspringbootjdbc.domain.comment.Body as CommentBody
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat

class CommentTest {
    @Test
    fun `Comment は識別子 CommentId が一致していたら、同値であることを期待する`() {
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
        val comment1 = Comment.newWithoutValidation(
            CommentId.newWithoutValidation(1),
            CommentBody.newWithoutValidation("dummy-body-1"),
            createdAt = date,
            updatedAt = date,
            OtherUser.newWithoutValidation(
                UserId(1),
                Username.newWithoutValidation("dummy-name1"),
                Bio.newWithoutValidation("dummy-bio1"),
                Image.newWithoutValidation("dummy-image1"),
                false
            )
        )
        val comment2 = Comment.newWithoutValidation(
            CommentId.newWithoutValidation(1),
            CommentBody.newWithoutValidation("dummy-body-2"),
            createdAt = date,
            updatedAt = date,
            OtherUser.newWithoutValidation(
                UserId(1),
                Username.newWithoutValidation("dummy-name1"),
                Bio.newWithoutValidation("dummy-bio1"),
                Image.newWithoutValidation("dummy-image1"),
                false
            )
        )
        val actual = comment1 == comment2
        assertThat(actual).isTrue
    }
}
