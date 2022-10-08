package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Some
import arrow.core.none
import com.example.realworldkotlinspringbootjdbc.presentation.request.NullableArticle
import com.example.realworldkotlinspringbootjdbc.presentation.response.Article
import com.example.realworldkotlinspringbootjdbc.presentation.response.Articles
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeMyErrorListForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
import com.example.realworldkotlinspringbootjdbc.presentation.shared.AuthorizationError
import com.example.realworldkotlinspringbootjdbc.usecase.article.CreateArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.DeleteCreatedArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.FeedUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.FilterCreatedArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.ShowArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.UpdateCreatedArticleUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ArticleController(
    val myAuth: MyAuth,
    val showArticle: ShowArticleUseCase,
    val filterCreatedArticle: FilterCreatedArticleUseCase,
    val createArticle: CreateArticleUseCase,
    val deleteArticle: DeleteCreatedArticleUseCase,
    val updateArticle: UpdateCreatedArticleUseCase,
    val feed: FeedUseCase,
) {

    /**
     *
     * 作成済み記事のフィルタ
     *
     * 例(成功/失敗)
     * $ curl -X GET --header 'Content-Type: application/json' 'http://localhost:8080/api/articles/' | jq '.'
     * $ curl -X GET --header 'Content-Type: application/json' 'http://localhost:8080/api/articles/?tag=lisp' | jq '.'
     * $ curl -X GET --header 'Content-Type: application/json' 'http://localhost:8080/api/articles/?tag=lisp&limit=2' | jq '.'
     *
     */
    @GetMapping("/articles")
    fun filter(
        @RequestHeader("Authorization") rawAuthorizationHeader: String? = null,
        @RequestParam(name = "tag", required = false) tag: String? = null,
        @RequestParam(name = "author", required = false) author: String? = null,
        @RequestParam(name = "favorited", required = false) favoritedByUsername: String? = null,
        @RequestParam(name = "limit", required = false) limit: String? = null,
        @RequestParam(name = "offset", required = false) offset: String? = null,
    ): ResponseEntity<String> {
        val optionalCurrentUser = myAuth.authorize(rawAuthorizationHeader).fold(
            { none() },
            { Some(it) }
        )
        return when (
            val useCaseResult = filterCreatedArticle.execute(
                tag,
                author,
                favoritedByUsername,
                limit,
                offset,
                optionalCurrentUser
            )
        ) {
            /**
             * フィルタ失敗
             */
            is Left -> when (val useCaseError = useCaseResult.value) {
                /**
                 * 原因: フィルタパラメータのバリデーションエラー
                 */
                is FilterCreatedArticleUseCase.Error.FilterParametersValidationErrors -> ResponseEntity(
                    serializeMyErrorListForResponseBody(useCaseError.errors),
                    HttpStatus.valueOf(422)
                )
                /**
                 * 原因: ユーザーがいなかった
                 * TODO: UseCase 的にありえないので、ここのエラーハンドリングは要検討
                 */
                is FilterCreatedArticleUseCase.Error.NotFoundUser -> ResponseEntity(
                    serializeUnexpectedErrorForResponseBody("ユーザー登録されていませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                    HttpStatus.valueOf(404)
                )
                /**
                 * 原因: offset値がフィルタ後の作成済み記事の数を超えている
                 */
                is FilterCreatedArticleUseCase.Error.OffsetOverCreatedArticlesCountError -> ResponseEntity(
                    serializeUnexpectedErrorForResponseBody(
                        """
                        offset値がフィルタした結果の作成済み記事の数を超えています(offset=${useCaseError.filterParameters.offset}, articlesCount=${useCaseError.articlesCount})
                        """.trimIndent()
                    ), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                    HttpStatus.valueOf(422)
                )
            }
            /**
             * フィルタ成功
             */
            is Right -> {
                ResponseEntity(
                    ObjectMapper().writeValueAsString(
                        Articles(
                            articlesCount = useCaseResult.value.articlesCount,
                            articles = useCaseResult.value.articles.map {
                                Article(
                                    title = it.article.title.value,
                                    slug = it.article.slug.value,
                                    body = it.article.body.value,
                                    createdAt = it.article.createdAt,
                                    updatedAt = it.article.updatedAt,
                                    description = it.article.description.value,
                                    tagList = it.article.tagList.map { tag -> tag.value },
                                    authorId = it.author.userId.value,
                                    favorited = it.article.favorited,
                                    favoritesCount = it.article.favoritesCount,
                                )
                            }
                        )
                    ),
                    HttpStatus.valueOf(200)
                )
            }
        }
    }

    @PostMapping("/articles")
    fun create(
        @RequestHeader("Authorization") rawAuthorizationHeader: String?,
        @RequestBody rawRequestBody: String?,
    ): ResponseEntity<String> {
        return when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
            /**
             * JWT 認証 失敗
             */
            is Left -> AuthorizationError.handle()

            /**
             * JWT 認証 成功
             */
            is Right -> {
                val article = NullableArticle.from(rawRequestBody)
                when (
                    val createdArticleWithAuthor = createArticle.execute(
                        authorizeResult.value,
                        article.title,
                        article.description,
                        article.body,
                        article.tagList
                    )
                ) {
                    /**
                     * 記事作成 失敗
                     */
                    is Left -> when (val useCaseError = createdArticleWithAuthor.value) {
                        /**
                         * 原因: バリデーションエラー
                         */
                        is CreateArticleUseCase.Error.InvalidArticle -> ResponseEntity(
                            serializeMyErrorListForResponseBody(useCaseError.errors),
                            HttpStatus.valueOf(422)
                        )
                    }
                    /**
                     * 記事作成 成功
                     */
                    is Right -> {
                        ResponseEntity(
                            Article.from(createdArticleWithAuthor.value).serializeWithRootName(),
                            HttpStatus.valueOf(200),
                        )
                    }
                }
            }
        }
    }

    /**
     * フォローしているユーザーの最新記事を取得
     *
     * - 認証: 必須
     * - クエリパラメータで制限可能
     *
     * @param rawRequestBody
     * @return
     */
    @GetMapping("/articles/feed")
    fun feed(
        @RequestHeader("Authorization") rawAuthorizationHeader: String?,
        @RequestParam(name = "limit", required = false) limit: String? = null,
        @RequestParam(name = "offset", required = false) offset: String? = null,
    ): ResponseEntity<String> {
        val currentUser = when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
            /**
             * 認証: 失敗
             */
            is Left -> return AuthorizationError.handle()
            /**
             * 認証: 成功
             */
            is Right -> authorizeResult.value
        }

        return when (
            val useCaseResult = feed.execute(
                currentUser = currentUser,
                limit = limit,
                offset = offset,
            )
        ) {
            /**
             * ユースケース: 失敗
             */
            is Left -> when (val error = useCaseResult.value) {
                /**
                 * 原因: フィードパラメータのバリデーションエラー
                 */
                is FeedUseCase.Error.FeedParameterValidationErrors -> ResponseEntity(
                    serializeMyErrorListForResponseBody(error.errors),
                    HttpStatus.valueOf(422)
                )
                /**
                 * 原因: Offset値が総記事数を超えている
                 */
                is FeedUseCase.Error.OffsetOverCreatedArticlesCountError -> ResponseEntity(
                    serializeUnexpectedErrorForResponseBody(
                        """
                            offset値が作成済み記事の数を超えています(offset=${error.feedParameters.offset}, articlesCount=${error.articlesCount})
                        """.trimIndent()
                    ), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                    HttpStatus.valueOf(422)
                )
            }
            /**
             * ユースケース: 成功
             */
            is Right -> ResponseEntity(
                ObjectMapper().writeValueAsString(
                    Articles(
                        articlesCount = useCaseResult.value.articlesCount,
                        articles = useCaseResult.value.articles.map {
                            Article(
                                title = it.article.title.value,
                                slug = it.article.slug.value,
                                body = it.article.body.value,
                                createdAt = it.article.createdAt,
                                updatedAt = it.article.updatedAt,
                                description = it.article.description.value,
                                tagList = it.article.tagList.map { tag -> tag.value },
                                authorId = it.author.userId.value,
                                favorited = it.article.favorited,
                                favoritesCount = it.article.favoritesCount,
                            )
                        }
                    )
                ),
                HttpStatus.valueOf(200)
            )
        }
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

    @PutMapping("/articles/{slug}")
    fun update(
        @RequestHeader("Authorization") rawAuthorizationHeader: String?,
        @PathVariable("slug") slug: String?,
        @RequestBody rawRequestBody: String?,
    ): ResponseEntity<String> {
        val author = when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
            /**
             * 認証: 失敗
             */
            is Left -> return AuthorizationError.handle()
            /**
             * 認証: 成功
             */
            is Right -> authorizeResult.value
        }

        val article = NullableArticle.from(rawRequestBody)

        return when (
            val updateArticleResult = updateArticle.execute(
                requestedUser = author,
                slug = slug,
                title = article.title,
                description = article.description,
                body = article.body,
            )
        ) {
            /**
             * 作成済み記事の更新: 失敗　
             */
            is Left -> when (val error = updateArticleResult.value) {
                /**
                 * 原因: 記事のバリデーションエラー
                 */
                is UpdateCreatedArticleUseCase.Error.InvalidArticle -> ResponseEntity(
                    serializeMyErrorListForResponseBody(error.errors),
                    HttpStatus.valueOf(422)
                )
                /**
                 * 原因: Slugのバリデーションエラー
                 */
                is UpdateCreatedArticleUseCase.Error.InvalidSlug -> ResponseEntity(
                    serializeMyErrorListForResponseBody(error.errors),
                    HttpStatus.valueOf(422)
                )
                /**
                 * 原因: 更新をしようとしたユーザーが著者ではなかった
                 */
                is UpdateCreatedArticleUseCase.Error.NotAuthor -> ResponseEntity(
                    serializeUnexpectedErrorForResponseBody("削除する権限がありません"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                    HttpStatus.valueOf(403)
                )
                /**
                 * 原因: 記事が見つからなかった
                 */
                is UpdateCreatedArticleUseCase.Error.NotFoundArticle -> ResponseEntity(
                    serializeUnexpectedErrorForResponseBody("記事が見つかりません　"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                    HttpStatus.valueOf(404)
                )
            }
            /**
             * 作成済み記事の更新: 成功
             */
            is Right -> ResponseEntity(
                Article.from(updateArticleResult.value).serializeWithRootName(),
                HttpStatus.valueOf(200)
            )
        }
    }

    @DeleteMapping("/articles/{slug}")
    fun delete(
        @RequestHeader("Authorization") rawAuthorizationHeader: String?,
        @PathVariable("slug") slug: String?
    ): ResponseEntity<String> {
        val author = when (val authorizeResult = myAuth.authorize(rawAuthorizationHeader)) {
            /**
             * 認証: 失敗
             */
            is Left -> return AuthorizationError.handle()
            /**
             * 認証: 成功
             */
            is Right -> authorizeResult.value
        }

        return when (
            val deleteResult = deleteArticle.execute(
                slug = slug,
                author = author
            )
        ) {
            /**
             * 削除: 失敗
             */
            is Left -> when (val error = deleteResult.value) {
                /**
                 * 原因: 著者ではなかった
                 */
                is DeleteCreatedArticleUseCase.Error.NotAuthor -> ResponseEntity(
                    serializeUnexpectedErrorForResponseBody("削除する権限がありません"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                    HttpStatus.valueOf(422)
                )
                /**
                 * 原因: 削除したい作成済み記事が見つからなかった
                 */
                is DeleteCreatedArticleUseCase.Error.NotFoundArticle -> ResponseEntity(
                    serializeUnexpectedErrorForResponseBody("記事が見つかりませんでした"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                    HttpStatus.valueOf(422)
                )
                /**
                 * 原因: バリデーションエラー
                 */
                is DeleteCreatedArticleUseCase.Error.ValidationError -> ResponseEntity(
                    serializeMyErrorListForResponseBody(error.errors),
                    HttpStatus.valueOf(422)
                )
            }

            /**
             * 削除: 成功
             */
            is Right -> ResponseEntity("", HttpStatus.valueOf(200))
        }
    }
}
