package com.example.realworldkotlinspringbootjdbc.presentation.request

import com.example.realworldkotlinspringbootjdbc.request.NullableUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NullableUserTest {
    @Test
    fun `引数がnullの場合、メンバが全てnullのNullableUserが取得できる`() {
        val actual = NullableUser.from(null)
        val expected = NullableUser(null, null, null, null, null)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `JSONのフォーマットがおかしい場合、メンバが全てnullのNullableUserが取得できる`() {
        val json = """
            {
                "user": {},,,,,
            }
        """.trimIndent()
        val actual = NullableUser.from(json)
        val expected = NullableUser(null, null, null, null, null)
        assertThat(actual).isEqualTo(expected)
    }

    @Nested
    class `引数のJSON文字列が"user"をkeyに持つ場合` {
        @Test
        fun `残りが空っぽの場合、メンバが全てnullのNullableUserが取得できる`() {
            val json = """
                {
                    "user": {}
                }
            """.trimIndent()
            val actual = NullableUser.from(json)
            val expected = NullableUser(null, null, null, null, null)
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `プロパティが揃っている場合、全てのプロパティを持つNullableUserが取得できる`() {
            val email = "dummy@example.com"
            val password = "dummy-pass"
            val username = "dummy-username"
            val bio = "dummy-bio"
            val image = "dummy-image"
            val json = """
                {
                    "user": {
                        "email": "$email",
                        "password": "$password",
                        "username": "$username",
                        "bio": "$bio",
                        "image": "$image"
                    }
                }
            """.trimIndent()
            val actual = NullableUser.from(json)
            val expected = NullableUser(email, password, username, bio, image)
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `想定していないプロパティがあっても無視したNullableUserが取得できる`() {
            val email = "dummy@example.com"
            val password = "dummy-pass"
            val username = "dummy-username"
            val bio = "dummy-bio"
            val image = "dummy-image"
            val json = """
                {
                    "user": {
                        "must-ignore": "must-ignore",
                        "email": "$email",
                        "password": "$password",
                        "username": "$username",
                        "bio": "$bio",
                        "image": "$image"
                    }
                }
            """.trimIndent()
            val actual = NullableUser.from(json)
            val expected = NullableUser(email, password, username, bio, image)
            assertThat(actual).isEqualTo(expected)
        }
    }
}
