package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Invalid
import arrow.core.Valid
import arrow.core.nonEmptyListOf
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.BioTest
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.EmailTest
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.ImageTest
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.domain.user.UsernameTest
import net.jqwik.api.ForAll
import net.jqwik.api.From
import net.jqwik.api.Property
import org.assertj.core.api.Assertions.assertThat

class RegisteredUserTest {
    class New {
        @Property
        fun `正常系-プロパティの値が全て有効な場合、検証済みの登録済みユーザーが戻り値`(
            @ForAll @From(supplier = EmailTest.EmailValidRange::class) email: String,
            @ForAll @From(supplier = UsernameTest.UsernameValidRange::class) username: String,
            @ForAll @From(supplier = BioTest.BioValidRange::class) bio: String,
            @ForAll @From(supplier = ImageTest.ImageValidRange::class) image: String,
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = RegisteredUser.new(
                1,
                email = email,
                username = username,
                bio = bio,
                image = image
            )

            /**
             * then:
             * - 成功していること
             * - 各プロパティが想定通りのプロパティにsetされていること
             */
            when (actual) {
                is Invalid -> assert(false) { "原因: ${actual.value}" }
                is Valid -> {
                    val registeredUser = actual.value
                    assertThat(registeredUser.userId.value).isEqualTo(1)
                    assertThat(registeredUser.email.value).isEqualTo(email)
                    assertThat(registeredUser.username.value).isEqualTo(username)
                    assertThat(registeredUser.bio.value).isEqualTo(bio)
                    assertThat(registeredUser.image.value).isEqualTo(image)
                }
            }
        }

        @Property
        fun `準正常系-プロパティの値が1つでも有効でない場合、それぞれのバリデーションエラーのListが戻り値`(
            @ForAll @From(supplier = EmailTest.EmailValidRange::class) email: String,
            @ForAll @From(supplier = UsernameTest.UsernameValidRange::class) username: String,
            @ForAll @From(supplier = BioTest.BioValidRange::class) bio: String,
            @ForAll @From(supplier = ImageTest.ImageValidRange::class) image: String,
        ) {
            /**
             * given:
             */

            /**
             * when:
             */
            val null1Actual = RegisteredUser.new(
                userId = 1,
                email = null,
                username = username,
                bio = bio,
                image = image
            )
            val null2Actual = RegisteredUser.new(
                userId = 2,
                email = email,
                username = null,
                bio = null,
                image = image
            )
            val null3Actual = RegisteredUser.new(
                userId = 3,
                email = null,
                username = null,
                bio = bio,
                image = null
            )
            val null4Actual = RegisteredUser.new(
                userId = 4,
                email = null,
                username = null,
                bio = null,
                image = null
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
                Username.ValidationError.Required,
                Bio.ValidationError.Required
            )
            val null3Expected = nonEmptyListOf(
                Email.ValidationError.Required,
                Username.ValidationError.Required,
                Image.ValidationError.Required
            )
            val null4Expected = nonEmptyListOf(
                Email.ValidationError.Required,
                Username.ValidationError.Required,
                Bio.ValidationError.Required,
                Image.ValidationError.Required
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
            when (null4Actual) {
                is Invalid -> assertThat(null4Actual.value).hasSameElementsAs(null4Expected)
                is Valid -> assert(false) { "準正常系のテストなので、バリデーションエラーを期待" }
            }
        }
    }
}
