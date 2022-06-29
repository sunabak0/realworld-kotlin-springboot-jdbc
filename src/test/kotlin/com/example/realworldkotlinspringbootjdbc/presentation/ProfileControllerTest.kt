package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Profile
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.usecase.ShowProfileUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class ProfileControllerTest {
    @Nested
    class ShowProfile {
        private val pathParam = "hoge-username"

        private inline fun profileController(showProfileUseCase: ShowProfileUseCase): ProfileController =
            ProfileController(showProfileUseCase)

        @Test
        fun `プロフィール取得時、UseCase が「Profile」を返す場合、200レスポンスを返す`() {
            val mockProfile = Profile.newWithoutValidation(
                Username.newWithoutValidation("hoge-username"),
                Bio.newWithoutValidation("hoge-bio"),
                Image.newWithoutValidation("hoge-image"),
                true
            )
            val showProfileReturnProfile = object : ShowProfileUseCase {
                override fun execute(username: String?): Either<ShowProfileUseCase.Error, Profile> =
                    mockProfile.right()
            }
            val actual = profileController(showProfileReturnProfile).showProfile(pathParam)
            val expected = ResponseEntity(
                """{"profile":{"username":"hoge-username","bio":"hoge-bio","image":"hoge-image","following":true}}""",
                HttpStatus.valueOf(200)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `プロフィール取得時、UseCase がバリデーションエラーを返す場合、404レスポンスを返す`() {
            val notImplementedValidationError = object : MyError.ValidationError {
                override val message: String get() = "DummyValidationError InvalidUserName"
                override val key: String get() = "DummyKey"
            }
            val showProfileReturnInvalidUserNameError = object : ShowProfileUseCase {
                override fun execute(username: String?): Either<ShowProfileUseCase.Error, Profile> =
                    ShowProfileUseCase.Error.InvalidUserName(listOf(notImplementedValidationError)).left()
            }
            val actual = profileController(showProfileReturnInvalidUserNameError).showProfile(pathParam)
            val expected = ResponseEntity(
                """{"errors":{"body":["プロフィールが見つかりませんでした"]}}""",
                HttpStatus.valueOf(404)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `プロフィール取得時、UseCase がNotFoundを返す場合、404レスポンスを返す`() {
            val notImplementedError = object : MyError {}
            val showProfileReturnNotFoundError = object : ShowProfileUseCase {
                override fun execute(username: String?): Either<ShowProfileUseCase.Error, Profile> =
                    ShowProfileUseCase.Error.NotFound(notImplementedError).left()
            }
            val actual = profileController(showProfileReturnNotFoundError).showProfile(pathParam)
            val expected = ResponseEntity(
                """{"errors":{"body":["プロフィールが見つかりませんでした"]}}""",
                HttpStatus.valueOf(404)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `プロフィール取得時、UseCase が原因不明のエラーを返す場合、500 レスポンスを返す`() {
            val notImplementedError = object : MyError {}
            val showProfileReturnUnexpectedError = object : ShowProfileUseCase {
                override fun execute(username: String?): Either<ShowProfileUseCase.Error, Profile> =
                    ShowProfileUseCase.Error.Unexpected(notImplementedError).left()
            }
            val actual = profileController(showProfileReturnUnexpectedError).showProfile(pathParam)
            val expected = ResponseEntity(
                """{"errors":{"body":["原因不明のエラーが発生しました"]}}""",
                HttpStatus.valueOf(500)
            )
            assertThat(actual).isEqualTo(expected)
        }
    }
}
