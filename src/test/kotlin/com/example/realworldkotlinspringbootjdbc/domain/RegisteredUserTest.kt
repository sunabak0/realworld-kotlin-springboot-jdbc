package com.example.realworldkotlinspringbootjdbc.domain

import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.StringLength
import org.assertj.core.api.Assertions.assertThat

class RegisteredUserTest {
    @Property
    fun `RegisteredUserを生成できる`(
        @ForAll @StringLength(min = 4, max = 32) username: String,
        @ForAll @StringLength(min = 0, max = 512) bio: String,
        @ForAll @StringLength(min = 0, max = 512) image: String,
    ) {
        // TODO Emailがいい感じに生成できるようにする
        val email = "hoge@example.com"
        val user = RegisteredUser.new(
            1,
            email,
            username,
            bio,
            image,
        )
        assertThat(user.isValid).isTrue
    }
}
