package com.example.realworldkotlinspringbootjdbc.presentation

import com.example.realworldkotlinspringbootjdbc.openapi.generated.controller.FavoritesApi
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Article
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModel
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModelErrors
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Profile
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.SingleArticleResponse
import com.example.realworldkotlinspringbootjdbc.presentation.shared.RealworldAuthenticationUseCaseUnauthorizedException
import com.example.realworldkotlinspringbootjdbc.usecase.favorite.FavoriteUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.favorite.UnfavoriteUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.shared.RealworldAuthenticationUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneOffset

@RestController
class FavoriteController(
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
                Article(
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
            { throw UnfavoriteUseCaseErrorException(it) },
            { it }
        )

        return ResponseEntity(
            SingleArticleResponse(
                Article(
                    slug = unfavoriteUseCaseResult.article.slug.value,
                    title = unfavoriteUseCaseResult.article.title.value,
                    description = unfavoriteUseCaseResult.article.description.value,
                    body = unfavoriteUseCaseResult.article.body.value,
                    tagList = unfavoriteUseCaseResult.article.tagList.map { it.value },
                    createdAt = unfavoriteUseCaseResult.article.createdAt.toInstant().atOffset(ZoneOffset.UTC),
                    updatedAt = unfavoriteUseCaseResult.article.updatedAt.toInstant().atOffset(ZoneOffset.UTC),
                    favorited = unfavoriteUseCaseResult.article.favorited,
                    favoritesCount = unfavoriteUseCaseResult.article.favoritesCount,
                    author = Profile(
                        username = unfavoriteUseCaseResult.author.username.value,
                        bio = unfavoriteUseCaseResult.author.bio.value,
                        image = unfavoriteUseCaseResult.author.image.value,
                        following = unfavoriteUseCaseResult.author.following
                    )
                )
            ),
            HttpStatus.OK
        )
    }

    data class UnfavoriteUseCaseErrorException(val error: UnfavoriteUseCase.Error) : Exception()

    @ExceptionHandler(value = [UnfavoriteUseCaseErrorException::class])
    fun onUnfavoriteUseCaseErrorException(e: UnfavoriteUseCaseErrorException): ResponseEntity<GenericErrorModel> =
        when (e.error) {
            /**
             * 原因: Slug がバリデーションエラー
             */
            is UnfavoriteUseCase.Error.InvalidSlug -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("slug が不正です"))),
                HttpStatus.UNPROCESSABLE_ENTITY
            )
            /**
             * 原因: 記事が見つからなかった
             */
            is UnfavoriteUseCase.Error.NotFound -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("記事が見つかりませんでした"))),
                HttpStatus.NOT_FOUND
            )
        }
}
