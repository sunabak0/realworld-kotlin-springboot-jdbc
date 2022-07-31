package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface FavoriteRepository {
    fun favorite(slug: Slug, currentUserId: UserId): Either<FavoriteError, CreatedArticle> = TODO()
    sealed interface FavoriteError : MyError {
        data class ArticleNotFoundBySlug(val slug: Slug) : FavoriteError, MyError.Basic
        data class Unexpected(override val cause: Throwable, val slug: Slug, val currentUserId: UserId) :
            FavoriteError,
            MyError.MyErrorWithThrowable
    }
}
