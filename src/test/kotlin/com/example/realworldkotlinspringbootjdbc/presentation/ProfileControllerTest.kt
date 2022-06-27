package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Profile
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.usecase.FollowProfileUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.ShowProfileUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
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

        private val notImplementedFollowProfileUseCase = object : FollowProfileUseCase {}
        private val notImplementedMyAuth = object : MyAuth {}

        private inline fun profileController(
            showProfileUseCase: ShowProfileUseCase,
            followProfileUseCase: FollowProfileUseCase,
            myAuth: MyAuth
        ): ProfileController =
            ProfileController(showProfileUseCase, followProfileUseCase, myAuth)

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
            val actual = profileController(
                showProfileReturnProfile,
                notImplementedFollowProfileUseCase,
                notImplementedMyAuth
            ).showProfile(pathParam)
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
            val actual =
                profileController(
                    showProfileReturnInvalidUserNameError,
                    notImplementedFollowProfileUseCase,
                    notImplementedMyAuth
                ).showProfile(pathParam)
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
            val actual = profileController(
                showProfileReturnNotFoundError,
                notImplementedFollowProfileUseCase,
                notImplementedMyAuth
            ).showProfile(pathParam)
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
            val actual =
                profileController(
                    showProfileReturnUnexpectedError,
                    notImplementedFollowProfileUseCase,
                    notImplementedMyAuth
                ).showProfile(pathParam)
            val expected = ResponseEntity(
                """{"errors":{"body":["原因不明のエラーが発生しました"]}}""",
                HttpStatus.valueOf(500)
            )
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    class Follow() {
        private val requestHeader = "hoge-authorize"
        private val pathParam = "hoge-username"
        val dummyRegisteredUser = RegisteredUser.newWithoutValidation(
            UserId(1),
            Email.newWithoutValidation("dummy@example.com"),
            Username.newWithoutValidation("dummy-name"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
        )
        private val notImplementedShowProfileUseCase = object : ShowProfileUseCase {}

        private inline fun profileController(
            showProfileUseCase: ShowProfileUseCase,
            followProfileUseCase: FollowProfileUseCase,
            myAuth: MyAuth
        ): ProfileController =
            ProfileController(showProfileUseCase, followProfileUseCase, myAuth)

        private val authorizedMyAuth = object : MyAuth {
            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                return dummyRegisteredUser.right()
            }
        }

        @Test
        fun `プロフォールをフォロー時、UseCaseが「Profile」を返す場合、200 レスポンスを返す`() {
            val returnProfile = Profile.newWithoutValidation(
                Username.newWithoutValidation("hoge-username"),
                Bio.newWithoutValidation("hoge-bio"),
                Image.newWithoutValidation("hoge-image"),
                true
            )
            val followProfileUseCase = object : FollowProfileUseCase {
                override fun execute(username: String?): Either<FollowProfileUseCase.Error, Profile> {
                    return returnProfile.right()
                }
            }
            val actual =
                profileController(notImplementedShowProfileUseCase, followProfileUseCase, authorizedMyAuth).follow(
                    requestHeader,
                    pathParam
                )
            val expected = ResponseEntity(
                """{"profile":{"username":"hoge-username","bio":"hoge-bio","image":"hoge-image","following":true}}""",
                HttpStatus.valueOf(200)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `プロフォールをフォロー時、UseCase がバリデーションエラーを返す場合、404 を返す`() {
            val notImplementedValidationError = object : MyError.ValidationError {
                override val message: String get() = "DummyValidationError InvalidUserName"
                override val key: String get() = "DummyKey"
            }
            val followProfileReturnInvalidUserNameError = object : FollowProfileUseCase {
                override fun execute(username: String?): Either<FollowProfileUseCase.Error, Profile> =
                    FollowProfileUseCase.Error.InvalidUserName(listOf(notImplementedValidationError)).left()
            }
            val actual =
                profileController(
                    notImplementedShowProfileUseCase,
                    followProfileReturnInvalidUserNameError,
                    authorizedMyAuth
                ).follow(requestHeader, pathParam)
            val expected = ResponseEntity(
                """{"errors":{"body":["プロフィールが見つかりませんでした"]}}""",
                HttpStatus.valueOf(404)
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `プロフォールをフォロー時、UseCase がNotFoundを返す場合、404レスポンスを返す`() {
            val notImplementedError = object : MyError {}
            val followProfileReturnNotFoundError = object : FollowProfileUseCase {
                override fun execute(username: String?): Either<FollowProfileUseCase.Error, Profile> =
                    FollowProfileUseCase.Error.NotFound(notImplementedError).left()
            }
            val actual = profileController(
                notImplementedShowProfileUseCase,
                followProfileReturnNotFoundError,
                authorizedMyAuth
            ).follow(requestHeader, pathParam)
            val expected = ResponseEntity(
                """{"errors":{"body":["プロフィールが見つかりませんでした"]}}""",
                HttpStatus.valueOf(404)
            )
            assertThat(actual).isEqualTo(expected)
        }
    }
}
