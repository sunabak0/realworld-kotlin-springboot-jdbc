package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Some
import arrow.core.getOrHandle
import arrow.core.handleError
import arrow.core.none
import arrow.core.toOption
import com.example.realworldkotlinspringbootjdbc.openapi.generated.controller.ArticlesApi
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModel
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModelErrors
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.MultipleArticlesResponse
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.NewArticleRequest
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Profile
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.SingleArticleResponse
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.UpdateArticleRequest
import com.example.realworldkotlinspringbootjdbc.presentation.response.Article
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.shared.RealworldAuthenticationUseCaseUnauthorizedException
import com.example.realworldkotlinspringbootjdbc.usecase.article.CreateArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.DeleteCreatedArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.FeedUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.FilterCreatedArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.ShowArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.UpdateCreatedArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.shared.RealworldAuthenticationUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneOffset

@RestController
class ArticleController(
    val myAuth: MyAuth,
    val realworldAuthenticationUseCase: RealworldAuthenticationUseCase,
    val showArticle: ShowArticleUseCase,
    val filterCreatedArticle: FilterCreatedArticleUseCase,
    val createArticle: CreateArticleUseCase,
    val deleteArticle: DeleteCreatedArticleUseCase,
    val updateArticle: UpdateCreatedArticleUseCase,
    val feed: FeedUseCase,
) : ArticlesApi {

    override fun getArticles(
        authorization: String?,
        tag: String?,
        author: String?,
        favorited: String?,
        limit: Int,
        offset: Int
    ): ResponseEntity<MultipleArticlesResponse> {
        /**
         * authorizationが
         *   - 無い(null) -> None
         *   - 有る -> 認証 -> 成功 -> Some<RegisteredUser>
         *   - 有る -> 認証 -> 失敗 -> 例外スロー
         */
        val currentUser = authorization.toOption().fold(
            { none() },
            {
                realworldAuthenticationUseCase.execute(it)
                    .getOrHandle { e -> throw RealworldAuthenticationUseCaseUnauthorizedException(e) }
                    .toOption()
            }
        )

        val filteredCreatedArticles = filterCreatedArticle.execute(
            tag = tag,
            author = author,
            favoritedByUsername = favorited,
            limit = limit.toString(),
            offset = offset.toString(),
            currentUser = currentUser
        ).getOrHandle { throw FilterCreatedArticleUseCaseErrorException(it) }

        return ResponseEntity(
            MultipleArticlesResponse(
                articlesCount = filteredCreatedArticles.articlesCount,
                articles = filteredCreatedArticles.articles.map {
                    com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Article(
                        slug = it.article.slug.value,
                        title = it.article.title.value,
                        description = it.article.description.value,
                        body = it.article.body.value,
                        tagList = it.article.tagList.map { tag -> tag.value },
                        createdAt = it.article.createdAt.toInstant().atOffset(ZoneOffset.UTC),
                        updatedAt = it.article.updatedAt.toInstant().atOffset(ZoneOffset.UTC),
                        favorited = it.article.favorited,
                        favoritesCount = it.article.favoritesCount,
                        author = Profile(
                            username = it.author.username.value,
                            bio = it.author.bio.value,
                            image = it.author.image.value,
                            following = it.author.following,
                        )
                    )
                }
            ),
            HttpStatus.OK
        )
    }

    data class FilterCreatedArticleUseCaseErrorException(val error: FilterCreatedArticleUseCase.Error) : Exception()

    @ExceptionHandler(value = [FilterCreatedArticleUseCaseErrorException::class])
    fun onFilterCreatedArticleUseCaseErrorException(e: FilterCreatedArticleUseCaseErrorException): ResponseEntity<GenericErrorModel> =
        when (val errorContents = e.error) {
            is FilterCreatedArticleUseCase.Error.FilterParametersValidationErrors -> ResponseEntity(
                GenericErrorModel(errors = GenericErrorModelErrors(body = errorContents.errors.map { it.message })),
                HttpStatus.UNPROCESSABLE_ENTITY
            )
            is FilterCreatedArticleUseCase.Error.NotFoundUser -> throw UnsupportedOperationException("ありえない")
            is FilterCreatedArticleUseCase.Error.OffsetOverCreatedArticlesCountError -> ResponseEntity(
                GenericErrorModel(
                    errors = GenericErrorModelErrors(
                        body = listOf(
                            "offset値がフィルタした結果の作成済み記事の数を超えています(offset=${errorContents.filterParameters.offset}, articlesCount=${errorContents.articlesCount})"
                        )
                    )
                ),
                HttpStatus.UNPROCESSABLE_ENTITY
            )
        }

    override fun createArticle(
        authorization: String,
        article: NewArticleRequest
    ): ResponseEntity<SingleArticleResponse> {
        val currentUser = realworldAuthenticationUseCase.execute(authorization)
            .getOrHandle { throw RealworldAuthenticationUseCaseUnauthorizedException(it) }

        val createdArticleWithAuthor = createArticle.execute(
            currentUser = currentUser,
            title = article.article.title,
            description = article.article.description,
            body = article.article.body,
            tagList = article.article.tagList,
        ).getOrHandle { throw CreateArticleUseCaseErrorException(it) }

        return ResponseEntity(
            SingleArticleResponse(
                article = com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Article(
                    slug = createdArticleWithAuthor.article.slug.value,
                    title = createdArticleWithAuthor.article.title.value,
                    description = createdArticleWithAuthor.article.description.value,
                    body = createdArticleWithAuthor.article.body.value,
                    tagList = createdArticleWithAuthor.article.tagList.map { it.value }.toList(),
                    createdAt = createdArticleWithAuthor.article.createdAt.toInstant().atOffset(ZoneOffset.UTC),
                    updatedAt = createdArticleWithAuthor.article.updatedAt.toInstant().atOffset(ZoneOffset.UTC),
                    favorited = createdArticleWithAuthor.article.favorited,
                    favoritesCount = createdArticleWithAuthor.article.favoritesCount,
                    author = Profile(
                        username = createdArticleWithAuthor.author.username.value,
                        bio = createdArticleWithAuthor.author.bio.value,
                        image = createdArticleWithAuthor.author.image.value,
                        following = createdArticleWithAuthor.author.following,
                    )
                )
            ),
            HttpStatus.CREATED
        )
    }

    data class CreateArticleUseCaseErrorException(val error: CreateArticleUseCase.Error) : Exception()

    @ExceptionHandler(value = [CreateArticleUseCaseErrorException::class])
    fun onCreateArticleUseCaseErrorException(e: CreateArticleUseCaseErrorException): ResponseEntity<GenericErrorModel> =
        when (val errorContents = e.error) {
            is CreateArticleUseCase.Error.InvalidArticle -> ResponseEntity(
                GenericErrorModel(errors = GenericErrorModelErrors(body = errorContents.errors.map { it.message }.toList())),
                HttpStatus.UNPROCESSABLE_ENTITY
            )
        }

    override fun getArticlesFeed(
        authorization: String,
        limit: Int,
        offset: Int
    ): ResponseEntity<MultipleArticlesResponse> {
        val currentUser = realworldAuthenticationUseCase.execute(authorization)
            .getOrHandle { throw RealworldAuthenticationUseCaseUnauthorizedException(it) }

        val feedCreatedArticles = feed.execute(
            currentUser = currentUser,
            limit = limit.toString(),
            offset = offset.toString(),
        ).getOrHandle { throw FeedUseCaseErrorException(it) }

        return ResponseEntity(
            MultipleArticlesResponse(
                articles = feedCreatedArticles.articles.map {
                    com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Article(
                        slug = it.article.slug.value,
                        title = it.article.title.value,
                        description = it.article.description.value,
                        body = it.article.body.value,
                        tagList = it.article.tagList.map { tag -> tag.value }.toList(),
                        createdAt = it.article.createdAt.toInstant().atOffset(ZoneOffset.UTC),
                        updatedAt = it.article.updatedAt.toInstant().atOffset(ZoneOffset.UTC),
                        favorited = it.article.favorited,
                        favoritesCount = it.article.favoritesCount,
                        author = Profile(
                            username = it.author.username.value,
                            bio = it.author.bio.value,
                            image = it.author.image.value,
                            following = it.author.following,
                        )
                    )
                }.toList(),
                articlesCount = feedCreatedArticles.articlesCount,
            ),
            HttpStatus.OK
        )
    }

    data class FeedUseCaseErrorException(val error: FeedUseCase.Error) : Exception()

    @ExceptionHandler(value = [FeedUseCaseErrorException::class])
    fun onFeedUseCaseErrorException(e: FeedUseCaseErrorException): ResponseEntity<GenericErrorModel> =
        when (val errorContents = e.error) {
            is FeedUseCase.Error.FeedParameterValidationErrors -> ResponseEntity(
                GenericErrorModel(errors = GenericErrorModelErrors(body = errorContents.errors.map { it.message }.toList())),
                HttpStatus.UNPROCESSABLE_ENTITY
            )
            is FeedUseCase.Error.OffsetOverCreatedArticlesCountError -> ResponseEntity(
                GenericErrorModel(
                    errors = GenericErrorModelErrors(
                        body = listOf(
                            """
                        offset値が作成済み記事の数を超えています(offset=${errorContents.feedParameters.offset}, articlesCount=${errorContents.articlesCount})
                            """.trimIndent()
                        )
                    )
                ),
                HttpStatus.UNPROCESSABLE_ENTITY
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
                    is ShowArticleUseCase.Error.NotFoundArticleBySlug -> ResponseEntity(
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
                     * 原因: ユーザーがいなかった
                     * TODO: UseCase 的にありえないので、ここのエラーハンドリングは要検討
                     */
                    is ShowArticleUseCase.Error.NotFoundUser -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("ユーザー登録されていませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                }
                /**
                 * 記事取得 成功
                 */
                is Right -> {
                    ResponseEntity(
                        Article.from(showArticleResult.value).serializeWithRootName(),
                        HttpStatus.valueOf(200),
                    )
                }
            }

            /**
             * JWT 認証 成功
             */
            is Right -> when (val showArticleResult = showArticle.execute(slug, Some(authorizeResult.value))) {
                /**
                 * 記事取得 失敗
                 */
                is Left -> when (showArticleResult.value) {
                    /**
                     * 原因: slug に該当する記事が見つからなかった
                     */
                    is ShowArticleUseCase.Error.NotFoundArticleBySlug -> ResponseEntity(
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
                     * 原因: ユーザーがいなかった
                     */
                    is ShowArticleUseCase.Error.NotFoundUser -> ResponseEntity(
                        serializeUnexpectedErrorForResponseBody("ユーザー登録されていませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                        HttpStatus.valueOf(404)
                    )
                }
                /**
                 * 記事取得 成功
                 */
                is Right -> {
                    ResponseEntity(
                        Article.from(showArticleResult.value).serializeWithRootName(),
                        HttpStatus.valueOf(200),
                    )
                }
            }
        }
    }

    override fun updateArticle(
        authorization: String,
        slug: String,
        article: UpdateArticleRequest
    ): ResponseEntity<SingleArticleResponse> {
        val currentUser = realworldAuthenticationUseCase.execute(authorization)
            .getOrHandle { throw RealworldAuthenticationUseCaseUnauthorizedException(it) }

        val updatedArticleWithAuthor = updateArticle.execute(
            requestedUser = currentUser,
            slug = slug,
            title = article.article.title,
            description = article.article.description,
            body = article.article.body
        ).getOrHandle { throw UpdateCreatedArticleUseCaseErrorException(it) }

        return ResponseEntity(
            SingleArticleResponse(
                article = com.example.realworldkotlinspringbootjdbc.openapi.generated.model.Article(
                    slug = updatedArticleWithAuthor.article.slug.value,
                    title = updatedArticleWithAuthor.article.title.value,
                    description = updatedArticleWithAuthor.article.description.value,
                    body = updatedArticleWithAuthor.article.body.value,
                    tagList = updatedArticleWithAuthor.article.tagList.map { it.value }.toList(),
                    createdAt = updatedArticleWithAuthor.article.createdAt.toInstant().atOffset(ZoneOffset.UTC),
                    updatedAt = updatedArticleWithAuthor.article.updatedAt.toInstant().atOffset(ZoneOffset.UTC),
                    favorited = updatedArticleWithAuthor.article.favorited,
                    favoritesCount = updatedArticleWithAuthor.article.favoritesCount,
                    author = Profile(
                        username = updatedArticleWithAuthor.author.username.value,
                        bio = updatedArticleWithAuthor.author.bio.value,
                        image = updatedArticleWithAuthor.author.image.value,
                        following = updatedArticleWithAuthor.author.following,
                    )
                )
            ),
            HttpStatus.OK
        )
    }

    data class UpdateCreatedArticleUseCaseErrorException(val error: UpdateCreatedArticleUseCase.Error) : Exception()

    @ExceptionHandler(value = [UpdateCreatedArticleUseCaseErrorException::class])
    fun onUpdateCreatedArticleUseCaseErrorException(e: UpdateCreatedArticleUseCaseErrorException): ResponseEntity<GenericErrorModel> =
        when (val errorContents = e.error) {
            is UpdateCreatedArticleUseCase.Error.InvalidArticle -> ResponseEntity(
                GenericErrorModel(errors = GenericErrorModelErrors(body = errorContents.errors.map { it.message }.toList())),
                HttpStatus.UNPROCESSABLE_ENTITY
            )
            is UpdateCreatedArticleUseCase.Error.InvalidSlug -> ResponseEntity(
                GenericErrorModel(errors = GenericErrorModelErrors(body = errorContents.errors.map { it.message }.toList())),
                HttpStatus.UNPROCESSABLE_ENTITY
            )
            is UpdateCreatedArticleUseCase.Error.NotAuthor -> ResponseEntity(
                GenericErrorModel(errors = GenericErrorModelErrors(body = listOf("削除する権限がありません"))),
                HttpStatus.FORBIDDEN
            )
            is UpdateCreatedArticleUseCase.Error.NotFoundArticle -> ResponseEntity(
                GenericErrorModel(errors = GenericErrorModelErrors(body = listOf("記事が見つかりません"))),
                HttpStatus.NOT_FOUND
            )
        }

    override fun deleteArticle(authorization: String, slug: String): ResponseEntity<Unit> {
        val currentUser = realworldAuthenticationUseCase.execute(authorization)
            .getOrHandle { throw RealworldAuthenticationUseCaseUnauthorizedException(it) }

        deleteArticle.execute(
            author = currentUser,
            slug = slug,
        ).handleError { throw DeleteCreatedArticleUseCaseErrorException(it) }

        return ResponseEntity(Unit, HttpStatus.OK)
    }

    data class DeleteCreatedArticleUseCaseErrorException(val error: DeleteCreatedArticleUseCase.Error) : Exception()

    @ExceptionHandler(value = [DeleteCreatedArticleUseCaseErrorException::class])
    fun onDeleteCreatedArticleUseCaseErrorException(e: DeleteCreatedArticleUseCaseErrorException): ResponseEntity<GenericErrorModel> =
        when (val errorContents = e.error) {
            is DeleteCreatedArticleUseCase.Error.NotAuthor -> ResponseEntity(
                GenericErrorModel(errors = GenericErrorModelErrors(body = listOf("削除する権限がありません"))),
                HttpStatus.UNPROCESSABLE_ENTITY
            )
            is DeleteCreatedArticleUseCase.Error.NotFoundArticle -> ResponseEntity(
                GenericErrorModel(errors = GenericErrorModelErrors(body = listOf("記事が見つかりませんでした"))),
                HttpStatus.UNPROCESSABLE_ENTITY
            )
            is DeleteCreatedArticleUseCase.Error.ValidationError -> ResponseEntity(
                GenericErrorModel(errors = GenericErrorModelErrors(body = errorContents.errors.map { it.message }.toList())),
                HttpStatus.UNPROCESSABLE_ENTITY
            )
        }
}
