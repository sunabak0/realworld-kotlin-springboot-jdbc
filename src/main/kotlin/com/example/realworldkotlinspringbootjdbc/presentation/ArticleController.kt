package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Some
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.presentation.response.Article
import com.example.realworldkotlinspringbootjdbc.presentation.response.Articles
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
import com.example.realworldkotlinspringbootjdbc.usecase.article.ShowArticleUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.text.SimpleDateFormat

@RestController
@Tag(name = "Articles")
class ArticleController(
    val myAuth: MyAuth,
    val showArticle: ShowArticleUseCase,
) {
    @GetMapping("/articles")
    fun filter(@Suppress("UnusedPrivateMember") @RequestBody rawRequestBody: String?): ResponseEntity<String> {
        val articles = Articles(
            1,
            listOf(
                Article(
                    "hoge-title",
                    "hoge-slug",
                    "hoge-body",
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    "hoge-description",
                    listOf("dragons", "training"),
                    1,
                    true,
                    1,
                )
            )
        )
        return ResponseEntity(
            ObjectMapper().writeValueAsString(articles),
            HttpStatus.valueOf(200)
        )
    }

    @Suppress("UnusedPrivateMember")
    @PostMapping("/articles")
    fun create(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        val article = Article(
            "hoge-title",
            "hoge-slug",
            "hoge-body",
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            "hoge-description",
            listOf("dragons", "training"),
            1,
            true,
            1,
        ).right()
        return ResponseEntity(
            ObjectMapper().enable(SerializationFeature.WRAP_ROOT_VALUE).writeValueAsString(article),
            HttpStatus.valueOf(200),
        )
    }

    @GetMapping("/articles/feed")
    fun feed(@Suppress("UnusedPrivateMember") @RequestBody rawRequestBody: String?): ResponseEntity<String> {
        val articles = Articles(
            1,
            listOf(
                Article(
                    "hoge-title",
                    "hoge-slug",
                    "hoge-body",
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    "hoge-description",
                    listOf("dragons", "training"),
                    1,
                    true,
                    1,
                )
            )
        )
        return ResponseEntity(
            ObjectMapper().writeValueAsString(articles),
            HttpStatus.valueOf(200)
        )
    }

    @GetMapping("/articles/{slug}")
    fun show(
        @RequestHeader("Authorization") rawAuthorizationHeader: String?,
        @PathVariable("slug") slug: String?
    ): ResponseEntity<String> {
        return when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
            /**
             * JWT 認証 失敗
             */
            is Left -> when (val showArticleResult = showArticle.execute(slug)) {
                /**
                 * 記事取得 失敗
                 */
                is Left -> when (showArticleResult.value) {
                    /**
                     * 原因: slug に該当する記事が見つからなかった
                     */
                    is ShowArticleUseCase.Error.NotFound -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                    /**
                     * 原因: バリデーションエラー
                     */
                    is ShowArticleUseCase.Error.ValidationErrors -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                    /**
                     * 原因: 不明
                     */
                    is ShowArticleUseCase.Error.Unexpected -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("原因不明のエラーが発生しました"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(500)
                    )
                }
                /**
                 * 記事取得 成功
                 */
                is Right -> {
                    val article = Article(
                        showArticleResult.value.title.value,
                        showArticleResult.value.slug.value,
                        showArticleResult.value.body.value,
                        showArticleResult.value.createdAt,
                        showArticleResult.value.updatedAt,
                        showArticleResult.value.description.value,
                        showArticleResult.value.tagList.map { tag -> tag.value },
                        // TODO: authorId を author に変更
                        showArticleResult.value.authorId.value,
                        showArticleResult.value.favorited,
                        showArticleResult.value.favoritesCount
                    )
                    ResponseEntity(
                        ObjectMapper().enable(SerializationFeature.WRAP_ROOT_VALUE).writeValueAsString(article),
                        HttpStatus.valueOf(200),
                    )
                }
            }

            /**
             * JWT 認証 成功
             */
            is Right -> when (val showArticleResult = showArticle.execute(slug, Some(authorizeResult.value))) {
                /**
                 * 記事取得 成功
                 */
                is Left -> when (showArticleResult.value) {
                    /**
                     * 原因: slug に該当する記事が見つからなかった
                     */
                    is ShowArticleUseCase.Error.NotFound -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                    /**
                     * 原因: バリデーションエラー
                     */
                    is ShowArticleUseCase.Error.ValidationErrors -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                    /**
                     * 原因: 不明
                     */
                    is ShowArticleUseCase.Error.Unexpected -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("原因不明のエラーが発生しました"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(500)
                    )
                }
                /**
                 * 記事取得 失敗
                 */
                is Right -> {
                    val article = Article(
                        showArticleResult.value.title.value,
                        showArticleResult.value.slug.value,
                        showArticleResult.value.body.value,
                        showArticleResult.value.createdAt,
                        showArticleResult.value.updatedAt,
                        showArticleResult.value.description.value,
                        showArticleResult.value.tagList.map { tag -> tag.value },
                        // TODO: authorId を author に変更
                        showArticleResult.value.authorId.value,
                        showArticleResult.value.favorited,
                        showArticleResult.value.favoritesCount
                    )
                    ResponseEntity(
                        ObjectMapper().enable(SerializationFeature.WRAP_ROOT_VALUE).writeValueAsString(article),
                        HttpStatus.valueOf(200),
                    )
                }
            }
        }
    }

    @PutMapping("/articles/{slug}")
    fun update(@Suppress("UnusedPrivateMember") @RequestBody rawRequestBody: String?): ResponseEntity<String> {
        val article = Article(
            "hoge-title",
            "hoge-slug",
            "hoge-body",
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            "hoge-description",
            listOf("dragons", "training"),
            1,
            true,
            1,
        )
        return ResponseEntity(
            ObjectMapper().enable(SerializationFeature.WRAP_ROOT_VALUE).writeValueAsString(article),
            HttpStatus.valueOf(200),
        )
    }

    @DeleteMapping("/articles/{slug}")
    fun delete(): ResponseEntity<String> {
        return ResponseEntity("", HttpStatus.valueOf(200))
    }
}
