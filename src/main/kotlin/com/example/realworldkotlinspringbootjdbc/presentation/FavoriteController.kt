package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.openapi.generated.controller.FavoritesApi
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModel
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModelErrors
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Profile
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.SingleArticleResponse
import com.example.realworldkotlinspringbootjdbc.presentation.response.Article
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.shared.AuthorizationError
import com.example.realworldkotlinspringbootjdbc.presentation.shared.RealworldAuthenticationUseCaseUnauthorizedException
import com.example.realworldkotlinspringbootjdbc.usecase.favorite.FavoriteUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.favorite.UnfavoriteUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.shared.RealworldAuthenticationUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.text.SimpleDateFormat
import java.time.ZoneOffset

@RestController
class FavoriteController(
    val myAuth: MyAuth,
    val realworldAuthenticationUseCase: RealworldAuthenticationUseCase,
    val favoriteUseCase: FavoriteUseCase,
    val unfavoriteUseCase: UnfavoriteUseCase,
) : FavoritesApi {
    override fun createArticleFavorite(authorization: String, slug: String): ResponseEntity<SingleArticleResponse> {
        val currentUser = realworldAuthenticationUseCase.execute(authorization).fold(
            { throw RealworldAuthenticationUseCaseUnauthorizedException(it) },
            { it }
        )

        val favoritedArticle = favoriteUseCase.execute(slug, currentUser).fold(
            { throw FavoriteArticleUseCaseErrorException(it) },
            { it }
        )

        return ResponseEntity(
            SingleArticleResponse(
                com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Article(
                    slug = favoritedArticle.slug.value,
                    title = favoritedArticle.title.value,
                    description = favoritedArticle.description.value,
                    body = favoritedArticle.body.value,
                    tagList = favoritedArticle.tagList.map { it.value },
                    createdAt = favoritedArticle.createdAt.toInstant().atOffset(ZoneOffset.UTC),
                    updatedAt = favoritedArticle.updatedAt.toInstant().atOffset(ZoneOffset.UTC),
                    favorited = favoritedArticle.favorited,
                    favoritesCount = favoritedArticle.favoritesCount,
                    author = Profile(
                        username = "dummy-username",
                        bio = "dummy-bio",
                        image = "dummy-image",
                        following = false
                    )
                )
            ),
            HttpStatus.OK
        )
    }

    data class FavoriteArticleUseCaseErrorException(val error: FavoriteUseCase.Error) : Exception()

    @ExceptionHandler(value = [FavoriteArticleUseCaseErrorException::class])
    fun onFavoriteArticleUseCaseErrorException(e: FavoriteArticleUseCaseErrorException): ResponseEntity<GenericErrorModel> =
        when (e.error) {
            /**
             * 原因: Slug がバリデーションエラー
             */
            is FavoriteUseCase.Error.InvalidSlug -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("slug が不正です"))),
                HttpStatus.UNPROCESSABLE_ENTITY
            )
            /**
             * 原因: 記事が見つからなかった
             */
            is FavoriteUseCase.Error.NotFoundArticleBySlug -> TODO()
        }
    // TODO: 以下の項目を実装したら削除
    //  - CreatedArticleWithAuthor を取得するクエリモデルが実装
    //  - 準正常系を実装
    // @PostMapping("/articles/{slug}/favorite")
    // fun favorite(
    //     @RequestHeader("Authorization") rawAuthorizationHeader: String?,
    //     @PathVariable("slug") slug: String?
    // ): ResponseEntity<String> {
    //     return when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
    //         /**
    //          * JWT 認証 失敗
    //          */
    //         is Left -> AuthorizationError.handle()
    //         /**
    //          * JWT 認証 成功
    //          */
    //         is Right -> {
    //             when (val favoritedArticle = favoriteUseCase.execute(slug, authorizeResult.value)) {
    //                 /**
    //                  * お気に入り登録 失敗
    //                  */
    //                 is Left -> when (favoritedArticle.value) {
    //                     /**
    //                      * 原因: Slug がバリデーションエラー
    //                      */
    //                     is FavoriteUseCase.Error.InvalidSlug -> ResponseEntity(
    //                         serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
    //                         HttpStatus.valueOf(404)
    //                     )
    //                     /**
    //                      * 原因: 記事が見つからなかった
    //                      */
    //                     is FavoriteUseCase.Error.NotFoundArticleBySlug -> ResponseEntity(
    //                         serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
    //                         HttpStatus.valueOf(404)
    //                     )
    //                 }
    //                 /**
    //                  * お気に入り登録 成功
    //                  */
    //                 is Right -> {
    //                     val article = Article(
    //                         favoritedArticle.value.title.value,
    //                         favoritedArticle.value.slug.value,
    //                         favoritedArticle.value.body.value,
    //                         favoritedArticle.value.createdAt,
    //                         favoritedArticle.value.updatedAt,
    //                         favoritedArticle.value.description.value,
    //                         favoritedArticle.value.tagList.map { tag -> tag.value },
    //                         // TODO: authorId を author に変更
    //                         favoritedArticle.value.authorId.value,
    //                         favoritedArticle.value.favorited,
    //                         favoritedArticle.value.favoritesCount
    //                     )
    //                     return ResponseEntity(
    //                         ObjectMapper().enable(SerializationFeature.WRAP_ROOT_VALUE).writeValueAsString(article),
    //                         HttpStatus.valueOf(200),
    //                     )
    //                 }
    //             }
    //         }
    //     }
    // }

    @DeleteMapping("/articles/{slug}/favorite")
    fun unfavorite(
        @RequestHeader("Authorization") rawAuthorizationHeader: String?,
        @PathVariable("slug") slug: String?
    ): ResponseEntity<String> {
        val article = Article(
            "hoge-title",
            "hoge-slug",
            "hoge-body",
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            "hoge-description",
            listOf("dragons", "training"),
            1,
            false,
            0,
        )
        return when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
            /**
             * JWT 認証 失敗
             */
            is Left -> AuthorizationError.handle()
            /**
             * JWT 認証 成功
             */
            is Right -> {
                when (val unfavoritedArticle = unfavoriteUseCase.execute(slug, authorizeResult.value)) {
                    /**
                     * お気に入り解除 失敗
                     */
                    is Left -> when (unfavoritedArticle.value) {
                        /**
                         * 原因: Slug が不正
                         */
                        is UnfavoriteUseCase.Error.InvalidSlug -> ResponseEntity(
                            serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                            HttpStatus.valueOf(404)
                        )
                        /**
                         * 原因: 記事が見つからなかった
                         */
                        is UnfavoriteUseCase.Error.NotFoundArticleBySlug -> ResponseEntity(
                            serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                            HttpStatus.valueOf(404)
                        )
                    }
                    /**
                     * お気に入り解除 成功
                     */
                    is Right -> {
                        val article = Article(
                            unfavoritedArticle.value.title.value,
                            unfavoritedArticle.value.slug.value,
                            unfavoritedArticle.value.body.value,
                            unfavoritedArticle.value.createdAt,
                            unfavoritedArticle.value.updatedAt,
                            unfavoritedArticle.value.description.value,
                            unfavoritedArticle.value.tagList.map { tag -> tag.value },
                            // TODO: authorId を author に変更
                            unfavoritedArticle.value.authorId.value,
                            unfavoritedArticle.value.favorited,
                            unfavoritedArticle.value.favoritesCount
                        )
                        return ResponseEntity(
                            ObjectMapper().enable(SerializationFeature.WRAP_ROOT_VALUE).writeValueAsString(article),
                            HttpStatus.valueOf(200),
                        )
                    }
                }
            }
        }
    }
}
