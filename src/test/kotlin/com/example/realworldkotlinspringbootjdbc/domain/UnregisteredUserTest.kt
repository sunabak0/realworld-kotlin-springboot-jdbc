package com.example.realworldkotlinspringbootjdbc.domain

import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.StringLength
import org.assertj.core.api.Assertions.assertThat

class UnregisteredUserTest {
    @Property
    fun `UnregisteredUserを生成できる`(
        @ForAll @StringLength(min = 8, max = 32) password: String,
        @ForAll @StringLength(min = 4, max = 32) username: String,
    ) {
        // TODO Emailがいい感じに生成できるようにする
        val email = "hoge@example.com"
        val user = UnregisteredUser.new(email, password, username)
        assertThat(user.isValid).isTrue
    }
}
