package com.example.realworldkotlinspringbootjdbc.usecase.article

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticleAuthorVerification
import com.example.realworldkotlinspringbootjdbc.domain.DeleteCreatedArticleAndComments
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface DeleteCreatedArticleUseCase {
    fun execute(author: RegisteredUser, slug: String?): Either<Error, Unit> = throw NotImplementedError()

    sealed interface Error : MyError {
        data class ValidationError(override val errors: List<MyError.ValidationError>) :
            Error, MyError.ValidationErrors
        data class NotFoundArticle(val slug: Slug) : Error, MyError.Basic
        data class NotAuthor(
            override val cause: MyError,
            val targetArticle: CreatedArticle,
            val notAuthorizedUser: RegisteredUser,
        ) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class DeleteCreatedArticleUseCaseImpl(
    val articleRepository: ArticleRepository,
    val deleteCreatedArticleAndComments: DeleteCreatedArticleAndComments,
) : DeleteCreatedArticleUseCase {
    override fun execute(author: RegisteredUser, slug: String?): Either<DeleteCreatedArticleUseCase.Error, Unit> {
        /**
         * バリデーション
         * Invalid -> 早期リターン
         */
        val validatedSlug = Slug.new(slug).fold(
            { return DeleteCreatedArticleUseCase.Error.ValidationError(errors = it).left() },
            { it }
        )

        /**
         * 検索
         * NotFound -> 早期リターン
         */
        val foundArticle = when (val targetArticle = articleRepository.findBySlug(validatedSlug)) {
            is Left -> when (targetArticle.value) {
                is ArticleRepository.FindBySlugError.NotFound -> return DeleteCreatedArticleUseCase.Error.NotFoundArticle(
                    slug = validatedSlug
                ).left()
                is ArticleRepository.FindBySlugError.Unexpected -> TODO("想定外なので、TODOにしておく")
            }
            is Right -> targetArticle.value
        }

        /**
         * 著者かどうか
         * 著者ではない -> 早期リターン
         * 著者である -> 削除
         */
        return when (
            val result = CreatedArticleAuthorVerification.verify(
                article = foundArticle,
                user = author
            )
        ) {
            /**
             * 著者ではない
             */
            is Left -> DeleteCreatedArticleUseCase.Error.NotAuthor(
                cause = result.value,
                targetArticle = foundArticle,
                notAuthorizedUser = author
            ).left()

            /**
             * 著者である -> 削除
             */
            is Right -> when (val deleteResult = deleteCreatedArticleAndComments.execute(foundArticle.id)) {
                /**
                 * 削除: 失敗
                 */
                is Left -> when (deleteResult.value) {
                    is DeleteCreatedArticleAndComments.Error.NotFoundArticle -> DeleteCreatedArticleUseCase.Error.NotFoundArticle(
                        slug = validatedSlug
                    ).left()
                }
                /**
                 * 削除: 成功
                 */
                is Right -> Unit.right()
            }
        }
    }
}
