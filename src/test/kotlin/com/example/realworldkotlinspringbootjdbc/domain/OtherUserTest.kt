package com.example.realworldkotlinspringbootjdbc.domain

import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OtherUserTest {
    @Test
    fun `OtherUser は識別子 UserId が一致していたら、同値であることを期待する`() {
        val user1 = OtherUser.newWithoutValidation(
            UserId(1),
            Username.newWithoutValidation("dummy-name1"),
            Bio.newWithoutValidation("dummy-bio1"),
            Image.newWithoutValidation("dummy-image1"),
            false
        )
        val user2 = OtherUser.newWithoutValidation(
            UserId(1),
            Username.newWithoutValidation("dummy-name2"),
            Bio.newWithoutValidation("dummy-bio2"),
            Image.newWithoutValidation("dummy-image2"),
            false
        )
        val actual = user1 == user2
        assertThat(actual).isTrue
    }
}
