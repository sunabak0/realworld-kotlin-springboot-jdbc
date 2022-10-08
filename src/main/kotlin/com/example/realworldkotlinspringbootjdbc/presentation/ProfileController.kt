package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.None
import arrow.core.Some
import com.example.realworldkotlinspringbootjdbc.presentation.response.Profile
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.shared.AuthorizationError
import com.example.realworldkotlinspringbootjdbc.usecase.profile.FollowProfileUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.profile.ShowProfileUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.profile.UnfollowProfileUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import javax.websocket.server.PathParam

@RestController
class ProfileController(
    val myAuth: MyAuth,
    val showProfileUseCase: ShowProfileUseCase,
    val followProfileUseCase: FollowProfileUseCase,
    val unfollowProfileUseCase: UnfollowProfileUseCase,
) {
    @GetMapping("/profiles/{username}")
    fun showProfile(
        @RequestHeader("Authorization") rawAuthorizationHeader: String?,
        @PathParam("username") username: String?
    ): ResponseEntity<String> {
        return when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
            /**
             * JWT 認証 失敗 or 未ログイン
             */
            is Left -> when (val result = showProfileUseCase.execute(username, None)) {
                /**
                 * プロフィール取得に失敗
                 */
                is Left -> when (result.value) {
                    /**
                     * 原因: バリデーションエラー
                     */
                    is ShowProfileUseCase.Error.InvalidUsername -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("プロフィールが見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                    /**
                     * 原因: プロフィールが見つからなかった
                     */
                    is ShowProfileUseCase.Error.NotFound -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("プロフィールが見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                }
                /**
                 * プロフィール取得に成功
                 */
                is Right -> {
                    val profile = Profile(
                        result.value.username.value,
                        result.value.bio.value,
                        result.value.image.value,
                        result.value.following
                    )
                    ResponseEntity(
                        ObjectMapper()
                            .enable(SerializationFeature.WRAP_ROOT_VALUE)
                            .writeValueAsString(profile),
                        HttpStatus.valueOf(200)
                    )
                }
            }
            /**
             * JWT 認証 成功
             */
            is Right -> when (val result = showProfileUseCase.execute(username, Some(authorizeResult.value))) {
                /**
                 * プロフィール取得に成功
                 */
                is Right -> {
                    val profile = Profile(
                        result.value.username.value,
                        result.value.bio.value,
                        result.value.image.value,
                        result.value.following
                    )
                    ResponseEntity(
                        ObjectMapper()
                            .enable(SerializationFeature.WRAP_ROOT_VALUE)
                            .writeValueAsString(profile),
                        HttpStatus.valueOf(200)
                    )
                }
                /**
                 * プロフィール取得に失敗
                 */
                is Left -> when (result.value) {
                    /**
                     * 原因: バリデーションエラー
                     */
                    is ShowProfileUseCase.Error.InvalidUsername -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("プロフィールが見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                    /**
                     * 原因: プロフィールが見つからなかった
                     */
                    is ShowProfileUseCase.Error.NotFound -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("プロフィールが見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                }
            }
        }
    }

    @PostMapping("/profiles/{username}/follow")
    fun follow(
        @RequestHeader("Authorization") rawAuthorizationHeader: String?,
        @PathParam("username") username: String?
    ): ResponseEntity<String> {
        return when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
            /**
             * JWT 認証 失敗
             */
            is Left -> AuthorizationError.handle(authorizeResult.value)
            /**
             * JWT 認証 成功
             */
            is Right -> when (
                val followedProfile =
                    followProfileUseCase.execute(username, authorizeResult.value)
            ) {
                /**
                 * プロフィールのフォローに失敗
                 */
                is Left -> when (@Suppress("UnusedPrivateMember") val useCaseError = followedProfile.value) {
                    /**
                     * 原因: Username が不正
                     */
                    is FollowProfileUseCase.Error.InvalidUsername -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("プロフィールが見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                    /**
                     * 原因: Profile が見つからなかった
                     */
                    is FollowProfileUseCase.Error.NotFound -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("プロフィールが見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                }
                /**
                 * プロフィールのフォローに成功
                 */
                is Right -> ResponseEntity(
                    Profile.from(followedProfile.value).serializeWithRootName(),
                    HttpStatus.valueOf(200)
                )
            }
        }
    }

    @DeleteMapping("/profiles/{username}/follow")
    fun unfollow(
        @RequestHeader("Authorization") rawAuthorizationHeader: String?,
        @PathParam("username") username: String?
    ): ResponseEntity<String> {
        return when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
            /**
             * JWT 認証 失敗
             */
            is Left -> AuthorizationError.handle(authorizeResult.value)
            /**
             * JWT 認証 成功
             */
            is Right -> when (
                val unfollowedProfile =
                    unfollowProfileUseCase.execute(username, authorizeResult.value)
            ) {
                /**
                 * プロフィールのアンフォローに失敗
                 */
                is Left -> when (unfollowedProfile.value) {
                    /**
                     * 原因: Username が不正
                     */
                    is UnfollowProfileUseCase.Error.InvalidUsername -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("プロフィールが見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                    /**
                     * 原因: Profile が見つからなかった
                     */
                    is UnfollowProfileUseCase.Error.NotFound -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("プロフィールが見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                }
                /**
                 * プロフィールのアンフォローに成功
                 */
                is Right -> ResponseEntity(
                    Profile.from(unfollowedProfile.value).serializeWithRootName(),
                    HttpStatus.valueOf(200)
                )
            }
        }
    }
}
