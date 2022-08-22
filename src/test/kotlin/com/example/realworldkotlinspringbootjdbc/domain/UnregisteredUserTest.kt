package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Invalid
import arrow.core.Valid
import arrow.core.nonEmptyListOf
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.EmailTest
import com.example.realworldkotlinspringbootjdbc.domain.user.Password
import com.example.realworldkotlinspringbootjdbc.domain.user.PasswordTest
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.domain.user.UsernameTest
import net.jqwik.api.ForAll
import net.jqwik.api.From
import net.jqwik.api.Property
import org.assertj.core.api.Assertions.assertThat

class UnregisteredUserTest {
    class New {
        @Property
        fun `正常系-属性の値が全て有効な場合、検証済みの未登録ユーザーが戻り値`(
            @ForAll @From(supplier = EmailTest.EmailValidRange::class) email: String,
            @ForAll @From(supplier = PasswordTest.PasswordValidRange::class) password: String,
            @ForAll @From(supplier = UsernameTest.UsernameValidRange::class) username: String
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = UnregisteredUser.new(
                email = email,
                password = password,
                username = username
            )

            /**
             * then:
             * - 成功していること
             * - 各属性が想定通りの属性に設定されていること
             */
            when (actual) {
                is Invalid -> assert(false) { "原因: ${actual.value}" }
                is Valid -> {
                    val user = actual.value
                    assertThat(user.email.value).isEqualTo(email)
                    assertThat(user.password.value).isEqualTo(password)
                    assertThat(user.username.value).isEqualTo(username)
                }
            }
        }

        @Property
        fun `準正常系-属性の値が1つでも有効でない場合、それぞれのバリデーションエラーのListが戻り値`(
            @ForAll @From(supplier = EmailTest.EmailValidRange::class) email: String,
            @ForAll @From(supplier = PasswordTest.PasswordValidRange::class) password: String,
            @ForAll @From(supplier = UsernameTest.UsernameValidRange::class) username: String
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val null1Actual = UnregisteredUser.new(
                email = null,
                password = password,
                username = username
            )
            val null2Actual = UnregisteredUser.new(
                email = email,
                password = null,
                username = null
            )
            val null3Actual = UnregisteredUser.new(
                email = null,
                password = null,
                username = null
            )

            /**
             * then:
             * - それぞれのバリデーションエラーのListである
             *   - 順番は意識したくない
             */
            val null1Expected = nonEmptyListOf(
                Email.ValidationError.Required
            )
            val null2Expected = nonEmptyListOf(
                Password.ValidationError.Required,
                Username.ValidationError.Required
            )
            val null3Expected = nonEmptyListOf(
                Email.ValidationError.Required,
                Password.ValidationError.Required,
                Username.ValidationError.Required
            )
            when (null1Actual) {
                is Invalid -> assertThat(null1Actual.value).hasSameElementsAs(null1Expected)
                is Valid -> assert(false) { "準正常系のテストなので、バリデーションエラーを期待" }
            }
            when (null2Actual) {
                is Invalid -> assertThat(null2Actual.value).hasSameElementsAs(null2Expected)
                is Valid -> assert(false) { "準正常系のテストなので、バリデーションエラーを期待" }
            }
            when (null3Actual) {
                is Invalid -> assertThat(null3Actual.value).hasSameElementsAs(null3Expected)
                is Valid -> assert(false) { "準正常系のテストなので、バリデーションエラーを期待" }
            }
        }
    }
}
