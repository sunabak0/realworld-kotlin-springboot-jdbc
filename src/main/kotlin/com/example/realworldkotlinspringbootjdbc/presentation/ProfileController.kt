package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Some
import arrow.core.none
import arrow.core.toOption
import com.example.realworldkotlinspringbootjdbc.openapi.generated.controller.ProfileApi
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModel
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModelErrors
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Profile
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.ProfileResponse
import com.example.realworldkotlinspringbootjdbc.presentation.shared.RealworldAuthenticationUseCaseUnauthorizedException
import com.example.realworldkotlinspringbootjdbc.usecase.profile.FollowProfileUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.profile.ShowProfileUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.profile.UnfollowProfileUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.shared.RealworldAuthenticationUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController

@RestController
class ProfileController(
    val realworldAuthenticationUseCase: RealworldAuthenticationUseCase,
    val showProfileUseCase: ShowProfileUseCase,
    val followProfileUseCase: FollowProfileUseCase,
    val unfollowProfileUseCase: UnfollowProfileUseCase,
) : ProfileApi {
    override fun getProfileByUsername(username: String, authorization: String?): ResponseEntity<ProfileResponse> {
        /**
         * JWT 認証
         * - Authorization ヘッダーなし -> none
         * - Authorization ヘッダーあり、JWT 認証失敗 -> none
         * - Authorization ヘッダーあり、JWT 認証成功 -> RegisteredUser
         */
        val currentUser = authorization.toOption().fold(
            { none() },
            {
                realworldAuthenticationUseCase.execute(it).fold(
                    { none() },
                    { result -> Some(result) }
                )
            }
        )

        val showProfileUseCaseResult = showProfileUseCase.execute(username, currentUser).fold(
            { throw ShowProfileUseCaseErrorException(it) },
            { it }
        )

        return ResponseEntity(
            ProfileResponse(
                Profile(
                    username = showProfileUseCaseResult.username.value,
                    bio = showProfileUseCaseResult.bio.value,
                    image = showProfileUseCaseResult.image.value,
                    following = showProfileUseCaseResult.following
                ),
            ),
            HttpStatus.OK
        )
    }

    data class ShowProfileUseCaseErrorException(val error: ShowProfileUseCase.Error) : Exception()

    @ExceptionHandler(value = [ShowProfileUseCaseErrorException::class])
    fun onShowProfileUseCaseErrorException(e: ShowProfileUseCaseErrorException): ResponseEntity<GenericErrorModel> =
        when (val error = e.error) {
            /**
             * Username が不正だった場合
             */
            is ShowProfileUseCase.Error.InvalidUsername -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = error.errors.map { it.message })),
                HttpStatus.UNPROCESSABLE_ENTITY
            )
            /**
             * Username に該当する登録済ユーザーが見つからなかった場合
             */
            is ShowProfileUseCase.Error.NotFound -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("プロフィールが見つかりませんでした"))),
                HttpStatus.NOT_FOUND
            )
        }

    override fun followUserByUsername(authorization: String, username: String): ResponseEntity<ProfileResponse> {
        /**
         * JWT 認証
         */
        val currentUser = realworldAuthenticationUseCase.execute(authorization).fold(
            { throw RealworldAuthenticationUseCaseUnauthorizedException(it) },
            { it }
        )

        val followProfileResult = followProfileUseCase.execute(username, currentUser).fold(
            { throw FollowProfileUseCaseErrorException(it) },
            { it }
        )
        return ResponseEntity(
            ProfileResponse(
                Profile(
                    username = followProfileResult.username.value,
                    bio = followProfileResult.bio.value,
                    image = followProfileResult.image.value,
                    following = followProfileResult.following
                ),
            ),
            HttpStatus.OK
        )
    }

    data class FollowProfileUseCaseErrorException(val error: FollowProfileUseCase.Error) : Exception()

    @ExceptionHandler(value = [FollowProfileUseCaseErrorException::class])
    fun onFollowProfileUseCaseErrorException(e: FollowProfileUseCaseErrorException): ResponseEntity<GenericErrorModel> =
        when (val error = e.error) {
            /**
             * Username が不正だった場合
             */
            is FollowProfileUseCase.Error.InvalidUsername -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = error.errors.map { it.message })),
                HttpStatus.UNPROCESSABLE_ENTITY
            )

            /**
             * Username に該当する登録済ユーザーが見つからなかった場合
             */
            is FollowProfileUseCase.Error.NotFound -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("プロフィールが見つかりませんでした"))),
                HttpStatus.NOT_FOUND
            )
        }

    override fun unfollowUserByUsername(authorization: String, username: String): ResponseEntity<ProfileResponse> {
        /**
         * JWT 認証
         */
        val currentUser = realworldAuthenticationUseCase.execute(authorization).fold(
            { throw RealworldAuthenticationUseCaseUnauthorizedException(it) },
            { it }
        )

        val unfollowProfileResult = unfollowProfileUseCase.execute(username, currentUser).fold(
            { throw UnfollowProfileUseCaseErrorException(it) },
            { it }
        )
        return ResponseEntity(
            ProfileResponse(
                Profile(
                    username = unfollowProfileResult.username.value,
                    bio = unfollowProfileResult.bio.value,
                    image = unfollowProfileResult.image.value,
                    following = unfollowProfileResult.following
                ),
            ),
            HttpStatus.OK
        )
    }

    data class UnfollowProfileUseCaseErrorException(val error: UnfollowProfileUseCase.Error) : Exception()

    @ExceptionHandler(value = [UnfollowProfileUseCaseErrorException::class])
    fun onUnfollowProfileUseCaseErrorException(e: UnfollowProfileUseCaseErrorException): ResponseEntity<GenericErrorModel> =
        when (val error = e.error) {
            /**
             * Username が不正だった場合
             */
            is UnfollowProfileUseCase.Error.InvalidUsername -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = error.errors.map { it.message })),
                HttpStatus.UNPROCESSABLE_ENTITY
            )

            is UnfollowProfileUseCase.Error.NotFound -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("プロフィールが見つかりませんでした"))),
                HttpStatus.NOT_FOUND
            )
        }
}
