package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface ArticleRepository {
    fun findBySlug(slug: Slug): Either<FindBySlugError, CreatedArticle> = TODO()
    sealed interface FindBySlugError : MyError {
        data class NotFound(val slug: Slug) : FindBySlugError, MyError.Basic
        data class Unexpected(override val cause: Throwable, val slug: Slug) : FindBySlugError, MyError.MyErrorWithThrowable
    }

    /**
     * タグ一覧
     */
    fun tags(): Either<TagsError, List<Tag>> = TODO("テストでDIする時、余計なoverrideを記述不要にするため")
    sealed interface TagsError : MyError {
        data class Unexpected(override val cause: Throwable) : TagsError, MyError.MyErrorWithThrowable
    }
}
