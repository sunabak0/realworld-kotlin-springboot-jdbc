package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Profile
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.usecase.ShowProfileUseCase
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
            val expected = ResponseEntity("""{"profile":{"username":"hoge-username","bio":"hoge-bio","image":"hoge-image","following":true}}""", HttpStatus.valueOf(200))
            assertThat(actual).isEqualTo(expected)
        }
    }
}
