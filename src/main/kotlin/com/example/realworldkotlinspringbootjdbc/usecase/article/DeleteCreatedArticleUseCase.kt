package com.example.realworldkotlinspringbootjdbc.usecase.article

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface DeleteCreatedArticleUseCase {
    fun execute(currentUser: RegisteredUser, slug: String?): Either<Error, Unit> = TODO()

    sealed interface Error : MyError {
        data class ValidationError(override val errors: List<MyError.ValidationError>) :
            Error, MyError.ValidationErrors
        data class NotFoundArticle(val slug: Slug) : Error, MyError.Basic
    }
}

@Service
class DeleteCreatedArticleUseCaseImpl(
    val articleRepository: ArticleRepository,
) : DeleteCreatedArticleUseCase {
    override fun execute(currentUser: RegisteredUser, slug: String?): Either<DeleteCreatedArticleUseCase.Error, Unit> {
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
         * 削除
         */
        return when (val deleteResult = articleRepository.delete(foundArticle.id)) {
            is Left -> when (deleteResult.value) {
                /**
                 * NotFound
                 */
                is ArticleRepository.DeleteError.NotFoundArticle -> DeleteCreatedArticleUseCase.Error.NotFoundArticle(
                    slug = validatedSlug
                ).left()
            }
            is Right -> Unit.right()
        }
    }
}
