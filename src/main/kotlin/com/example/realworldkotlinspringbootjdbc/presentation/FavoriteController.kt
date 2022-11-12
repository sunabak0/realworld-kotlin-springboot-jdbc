package com.example.realworldkotlinspringbootjdbc.presentation

import com.example.realworldkotlinspringbootjdbc.openapi.generated.controller.FavoritesApi
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModel
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModelErrors
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Profile
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.SingleArticleResponse
import com.example.realworldkotlinspringbootjdbc.presentation.response.Article
import com.example.realworldkotlinspringbootjdbc.presentation.shared.RealworldAuthenticationUseCaseUnauthorizedException
import com.example.realworldkotlinspringbootjdbc.usecase.favorite.FavoriteUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.favorite.UnfavoriteUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.shared.RealworldAuthenticationUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
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
                    slug = favoritedArticle.article.slug.value,
                    title = favoritedArticle.article.title.value,
                    description = favoritedArticle.article.description.value,
                    body = favoritedArticle.article.body.value,
                    tagList = favoritedArticle.article.tagList.map { it.value },
                    createdAt = favoritedArticle.article.createdAt.toInstant().atOffset(ZoneOffset.UTC),
                    updatedAt = favoritedArticle.article.updatedAt.toInstant().atOffset(ZoneOffset.UTC),
                    favorited = favoritedArticle.article.favorited,
                    favoritesCount = favoritedArticle.article.favoritesCount,
                    author = Profile(
                        username = favoritedArticle.author.username.value,
                        bio = favoritedArticle.author.bio.value,
                        image = favoritedArticle.author.image.value,
                        following = favoritedArticle.author.following
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
            is FavoriteUseCase.Error.NotFound -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("記事が見つかりませんでした"))),
                HttpStatus.NOT_FOUND
            )
        }

    override fun deleteArticleFavorite(authorization: String, slug: String): ResponseEntity<SingleArticleResponse> {
        val currentUser = realworldAuthenticationUseCase.execute(authorization).fold(
            { throw RealworldAuthenticationUseCaseUnauthorizedException(it) },
            { it }
        )

        val unfavoriteUseCaseResult = unfavoriteUseCase.execute(slug, currentUser).fold(
            { TODO() },
            { it }
        )

        return ResponseEntity(
            SingleArticleResponse(
                com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Article(
                    slug = unfavoriteUseCaseResult.slug.value,
                    title = unfavoriteUseCaseResult.title.value,
                    description = unfavoriteUseCaseResult.description.value,
                    body = unfavoriteUseCaseResult.body.value,
                    tagList = unfavoriteUseCaseResult.tagList.map { it.value },
                    createdAt = unfavoriteUseCaseResult.createdAt.toInstant().atOffset(ZoneOffset.UTC),
                    updatedAt = unfavoriteUseCaseResult.updatedAt.toInstant().atOffset(ZoneOffset.UTC),
                    favorited = unfavoriteUseCaseResult.favorited,
                    favoritesCount = unfavoriteUseCaseResult.favoritesCount,
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
    //
    // - レスポンスのauthorId
    /**
     * TODO: 以下が完了したら、コメントを削除
     * - レスポンスの author を dummy データではなく、DB のデータに変更
     * - 準正常系を実装
     */
    // @DeleteMapping("/articles/{slug}/favorite")
    // fun unfavorite(
    //     @RequestHeader("Authorization") rawAuthorizationHeader: String?,
    //     @PathVariable("slug") slug: String?
    // ): ResponseEntity<String> {
    //     val article = Article(
    //         "hoge-title",
    //         "hoge-slug",
    //         "hoge-body",
    //         SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
    //         SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
    //         "hoge-description",
    //         listOf("dragons", "training"),
    //         1,
    //         false,
    //         0,
    //     )
    //     return when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
    //         /**
    //          * JWT 認証 失敗
    //          */
    //         is Left -> AuthorizationError.handle()
    //         /**
    //          * JWT 認証 成功
    //          */
    //         is Right -> {
    //             when (val unfavoritedArticle = unfavoriteUseCase.execute(slug, authorizeResult.value)) {
    //                 /**
    //                  * お気に入り解除 失敗
    //                  */
    //                 is Left -> when (unfavoritedArticle.value) {
    //                     /**
    //                      * 原因: Slug が不正
    //                      */
    //                     is UnfavoriteUseCase.Error.InvalidSlug -> ResponseEntity(
    //                         serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
    //                         HttpStatus.valueOf(404)
    //                     )
    //                     /**
    //                      * 原因: 記事が見つからなかった
    //                      */
    //                     is UnfavoriteUseCase.Error.NotFoundArticleBySlug -> ResponseEntity(
    //                         serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
    //                         HttpStatus.valueOf(404)
    //                     )
    //                 }
    //                 /**
    //                  * お気に入り解除 成功
    //                  */
    //                 is Right -> {
    //                     val article = Article(
    //                         unfavoritedArticle.value.title.value,
    //                         unfavoritedArticle.value.slug.value,
    //                         unfavoritedArticle.value.body.value,
    //                         unfavoritedArticle.value.createdAt,
    //                         unfavoritedArticle.value.updatedAt,
    //                         unfavoritedArticle.value.description.value,
    //                         unfavoritedArticle.value.tagList.map { tag -> tag.value },
    //                         // TODO: authorId を author に変更
    //                         unfavoritedArticle.value.authorId.value,
    //                         unfavoritedArticle.value.favorited,
    //                         unfavoritedArticle.value.favoritesCount
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
}
