package com.example.realworldkotlinspringbootjdbc.usecase.article

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.usecase.shared_model.CreatedArticleWithAuthor
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service
import java.lang.UnsupportedOperationException

interface ShowArticleUseCase {
    fun execute(slug: String?, currentUser: Option<RegisteredUser> = None): Either<Error, CreatedArticleWithAuthor> = throw NotImplementedError()
    sealed interface Error : MyError {
        data class ValidationErrors(override val errors: List<MyError.ValidationError>) :
            Error,
            MyError.ValidationErrors

        data class NotFoundArticleBySlug(override val cause: MyError, val slug: Slug) :
            Error,
            MyError.MyErrorWithMyError

        data class NotFoundUser(override val cause: MyError, val user: RegisteredUser) :
            Error,
            MyError.MyErrorWithMyError
    }
}

@Service
class ShowArticleUseCaseImpl(
    val articleRepository: ArticleRepository,
    val profileRepository: ProfileRepository,
) : ShowArticleUseCase {
    override fun execute(
        slug: String?,
        currentUser: Option<RegisteredUser>
    ): Either<ShowArticleUseCase.Error, CreatedArticleWithAuthor> {
        /**
         * String -> Slug
         * 失敗 -> 早期return
         */
        val validatedSlug = Slug.new(slug).fold(
            { return ShowArticleUseCase.Error.ValidationErrors(it.all).left() },
            { it }
        )

        /**
         * 見つかった -> 作成済み記事
         * 見つからなかった -> 早期return
         * 観点となるユーザーが見つからなかった -> 早期return
         */
        val createdArticle = when (currentUser) {
            None -> articleRepository.findBySlug(validatedSlug).getOrHandle {
                return when (it) {
                    is ArticleRepository.FindBySlugError.NotFound -> ShowArticleUseCase.Error.NotFoundArticleBySlug(it, validatedSlug)
                }.left()
            }
            is Some -> articleRepository.findBySlugFromRegisteredUserViewpoint(validatedSlug, currentUser.value.userId).getOrHandle {
                return when (it) {
                    is ArticleRepository.FindBySlugFromRegisteredUserViewpointError.NotFoundArticle -> ShowArticleUseCase.Error.NotFoundArticleBySlug(it, validatedSlug)
                    is ArticleRepository.FindBySlugFromRegisteredUserViewpointError.NotFoundUser -> ShowArticleUseCase.Error.NotFoundUser(it, currentUser.value)
                }.left()
            }
        }

        /**
         * 著者の取得
         * - 必ず見つかるはず
         */
        val author = profileRepository.filterByUserIds(
            userIds = setOf(createdArticle.authorId),
            viewpointUserId = currentUser.map { it.userId }
        ).getOrHandle {
            throw UnsupportedOperationException("この分岐には入らない想定")
        }.first()

        return CreatedArticleWithAuthor(
            article = createdArticle,
            author = author,
        ).right()
    }
}
