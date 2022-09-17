package com.example.realworldkotlinspringbootjdbc.usecase.article

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticleAuthorVerification
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UpdatableCreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.usecase.shared_model.CreatedArticleWithAuthor
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

/**
 * 作成済み記事の更新
 *
 * - nullの場合はもともとのやつで更新する
 * - 変更する箇所がなくてもUpdateする(updated_atを更新)
 * - 著者じゃないと更新できない
 * - タグリストは更新できない(作成時そのまま)
 */
interface UpdateCreatedArticleUseCase {
    /**
     * 実行
     *
     * @param requestedUser リクエストしたユーザー(著者である必要がある)
     * @param slug Slug
     * @param title 更新したいタイトル(nullの場合はもとのやつが採用される)
     * @param description 更新したいdescription(同上)
     * @param body 更新したいbody(同上)
     * @return エラー or 著者情報付きの作成済み記事
     */
    fun execute(
        requestedUser: RegisteredUser,
        slug: String?,
        title: String?,
        description: String?,
        body: String?,
    ): Either<Error, CreatedArticleWithAuthor> = TODO()

    sealed interface Error : MyError {
        data class InvalidSlug(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class InvalidArticle(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class NotFoundArticle(val slug: Slug) : Error, MyError.Basic
        data class NotAuthor(
            override val cause: MyError,
            val targetArticle: CreatedArticle,
            val notAuthorizedUser: RegisteredUser,
        ) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class UpdateCreatedArticleUseCaseImpl(
    val articleRepository: ArticleRepository,
) : UpdateCreatedArticleUseCase {
    override fun execute(
        requestedUser: RegisteredUser,
        slug: String?,
        title: String?,
        description: String?,
        body: String?,
    ): Either<UpdateCreatedArticleUseCase.Error, CreatedArticleWithAuthor> {
        /**
         * String -> Slug
         * 失敗 -> 早期return
         */
        val validatedSlug = Slug.new(slug).fold(
            { return UpdateCreatedArticleUseCase.Error.InvalidSlug(errors = it).left() },
            { it }
        )

        /**
         * Slug -> CreatedArticle
         * 失敗 -> 早期return
         */
        val foundArticle = when (val result = articleRepository.findBySlug(validatedSlug)) {
            is Left -> when (result.value) {
                is ArticleRepository.FindBySlugError.NotFound -> return UpdateCreatedArticleUseCase.Error.NotFoundArticle(
                    slug = validatedSlug
                ).left()
                is ArticleRepository.FindBySlugError.Unexpected -> TODO("後々消すので、TODOにしておく")
            }
            is Right -> result.value
        }

        /**
         * 著者かどうかの確認
         * 著者ではない -> 早期return
         * 著者である -> 何もしない
         */
        when (val verifyResult = CreatedArticleAuthorVerification.verify(article = foundArticle, user = requestedUser)) {
            /**
             * 著者ではない -> 早期return
             */
            is Left -> return UpdateCreatedArticleUseCase.Error.NotAuthor(
                cause = verifyResult.value,
                targetArticle = foundArticle,
                notAuthorizedUser = requestedUser,
            ).left()
            /**
             * 著者である -> 更新可能な記事
             */
            is Right -> { /* 何もしない */ }
        }

        /**
         * 更新可能な作成済み記事
         * バリデーション: エラー -> 早期return
         */
        val updatableCreatedArticle = UpdatableCreatedArticle.new(
            originalCreatedArticle = foundArticle,
            title = title,
            description = description,
            body = body,
        ).fold(
            { return UpdateCreatedArticleUseCase.Error.InvalidArticle(errors = it).left() },
            { it }
        )

        /**
         * 作成済み記事の更新
         */
        return when (val result = articleRepository.update(updatableCreatedArticle)) {
            /**
             * 失敗
             */
            is Left -> when (result.value) {
                /**
                 * 原因: 記事が見つからなかった(DeleteとUpdateがほぼ同時にリクエストされた等)
                 */
                is ArticleRepository.UpdateError.NotFoundArticle -> UpdateCreatedArticleUseCase.Error.NotFoundArticle(validatedSlug).left()
            }
            /**
             * 成功
             */
            is Right -> {
                CreatedArticleWithAuthor(
                    article = CreatedArticle.newWithoutValidation(
                        id = updatableCreatedArticle.articleId,
                        title = updatableCreatedArticle.title,
                        slug = foundArticle.slug,
                        body = updatableCreatedArticle.body,
                        createdAt = foundArticle.createdAt,
                        updatedAt = updatableCreatedArticle.updatedAt,
                        description = updatableCreatedArticle.description,
                        tagList = foundArticle.tagList,
                        authorId = foundArticle.authorId,
                        favorited = foundArticle.favorited,
                        favoritesCount = foundArticle.favoritesCount,
                    ),
                    author = OtherUser.newWithoutValidation(
                        userId = requestedUser.userId,
                        username = requestedUser.username,
                        bio = requestedUser.bio,
                        image = requestedUser.image,
                        following = false
                    ),
                ).right()
            }
        }
    }
}
