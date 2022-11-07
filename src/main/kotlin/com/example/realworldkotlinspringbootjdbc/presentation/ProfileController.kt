package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Some
import arrow.core.none
import arrow.core.toOption
import com.example.realworldkotlinspringbootjdbc.openapi.generated.controller.ProfileApi
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModel
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModelErrors
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.ProfileResponse
import com.example.realworldkotlinspringbootjdbc.presentation.response.Profile
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.shared.AuthorizationError
import com.example.realworldkotlinspringbootjdbc.presentation.shared.RealworldAuthenticationUseCaseUnauthorizedException
import com.example.realworldkotlinspringbootjdbc.usecase.profile.FollowProfileUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.profile.ShowProfileUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.profile.UnfollowProfileUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.shared.RealworldAuthenticationUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import javax.websocket.server.PathParam

@RestController
class ProfileController(
    val myAuth: MyAuth,
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
                com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Profile(
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
                com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Profile(
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
            is FollowProfileUseCase.Error.NotFound -> TODO()
        }

    // @PostMapping("/profiles/{username}/follow")
    // fun follow(
    //     @RequestHeader("Authorization") rawAuthorizationHeader: String?,
    //     @PathParam("username") username: String?
    // ): ResponseEntity<String> {
    //     return when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
    //         /**
    //          * JWT 認証 失敗
    //          */
    //         is Left -> AuthorizationError.handle()
    //         /**
    //          * JWT 認証 成功
    //          */
    //         is Right -> when (
    //             val followedProfile =
    //                 followProfileUseCase.execute(username, authorizeResult.value)
    //         ) {
    //             /**
    //              * プロフィールのフォローに失敗
    //              */
    //             is Left -> when (@Suppress("UnusedPrivateMember") val useCaseError = followedProfile.value) {
    //                 /**
    //                  * 原因: Username が不正
    //                  */
    //                 is FollowProfileUseCase.Error.InvalidUsername -> ResponseEntity(
    //                     serializeUnexpectedErrorForResponseBody("プロフィールが見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
    //                     HttpStatus.valueOf(404)
    //                 )
    //                 /**
    //                  * 原因: Profile が見つからなかった
    //                  */
    //                 is FollowProfileUseCase.Error.NotFound -> ResponseEntity(
    //                     serializeUnexpectedErrorForResponseBody("プロフィールが見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
    //                     HttpStatus.valueOf(404)
    //                 )
    //             }
    //             /**
    //              * プロフィールのフォローに成功
    //              */
    //             is Right -> ResponseEntity(
    //                 Profile.from(followedProfile.value).serializeWithRootName(),
    //                 HttpStatus.valueOf(200)
    //             )
    //         }
    //     }
    // }

    @DeleteMapping("/profiles/{username}/follow")
    fun unfollow(
        @RequestHeader("Authorization") rawAuthorizationHeader: String?,
        @PathParam("username") username: String?
    ): ResponseEntity<String> {
        return when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
            /**
             * JWT 認証 失敗
             */
            is Left -> AuthorizationError.handle()
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
