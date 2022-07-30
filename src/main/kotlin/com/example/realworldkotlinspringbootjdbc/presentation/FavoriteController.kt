package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.presentation.response.Article
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.shared.AuthorizationError
import com.example.realworldkotlinspringbootjdbc.usecase.favorite.FavoriteUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.text.SimpleDateFormat

@RestController
@Tag(name = "Favorites")
class FavoriteController(
    val myAuth: MyAuth,
    val favoriteUseCase: FavoriteUseCase,
) {
    @PostMapping("/articles/{slug}/favorite")
    fun favorite(
        @RequestHeader("Authorization") rawAuthorizationHeader: String?,
        @PathVariable("slug") slug: String?
    ): ResponseEntity<String> {
        return when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
            /**
             * JWT 認証 失敗
             */
            is Left -> AuthorizationError.handle(authorizeResult.value)
            /**
             * JWT 認証 成功
             */
            is Right -> {
                when (val favoritedArticle = favoriteUseCase.execute(slug, authorizeResult.value)) {
                    /**
                     * お気に入り登録 失敗
                     */
                    is Left -> when (favoritedArticle.value) {
                        /**
                         * 原因: Slug がバリデーションエラー
                         */
                        is FavoriteUseCase.Error.InvalidSlug -> ResponseEntity(
                            serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                            HttpStatus.valueOf(404)
                        )
                        /**
                         * 原因: 記事が見つからなかった
                         */
                        is FavoriteUseCase.Error.ArticleNotFoundBySlug -> ResponseEntity(
                            serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                            HttpStatus.valueOf(404)
                        )
                        /**
                         * 原因: 不明
                         */
                        is FavoriteUseCase.Error.Unexpected -> ResponseEntity(
                            serializeUnexpectedErrorForResponseBody("原因不明のエラーが発生しました"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                            HttpStatus.valueOf(500)
                        )
                    }
                    /**
                     * お気に入り登録 成功
                     */
                    is Right -> {
                        val article = Article(
                            favoritedArticle.value.title.value,
                            favoritedArticle.value.slug.value,
                            favoritedArticle.value.body.value,
                            favoritedArticle.value.createdAt,
                            favoritedArticle.value.updatedAt,
                            favoritedArticle.value.description.value,
                            favoritedArticle.value.tagList.map { tag -> tag.value },
                            // TODO: authorId を author に変更
                            favoritedArticle.value.authorId.value.toString(),
                            favoritedArticle.value.favorited,
                            favoritedArticle.value.favoritesCount
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

    @DeleteMapping("/articles/{slug}/favorite")
    fun unfavorite(): ResponseEntity<String> {
        val article = Article(
            "hoge-title",
            "hoge-slug",
            "hoge-body",
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            "hoge-description",
            listOf("dragons", "training"),
            "hoge-author",
            false,
            0,
        )
        return ResponseEntity(
            ObjectMapper().enable(SerializationFeature.WRAP_ROOT_VALUE).writeValueAsString(article),
            HttpStatus.valueOf(200),
        )
    }
}
