package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface CommentRepository {
    fun list(slug: Slug): Either<ListError, List<Comment>> = TODO()
    sealed interface ListError : MyError {
        data class NotFoundArticleBySlug(val slug: Slug) : ListError, MyError.Basic
        data class Unexpected(override val cause: Throwable, val slug: Slug) : ListError, MyError.MyErrorWithThrowable
    }

    fun create(body: Body): Either<CreateError, Comment> = TODO()
    sealed interface CreateError : MyError {
        data class Unexpected(override val cause: Throwable, val body: Body) : CreateError, MyError.MyErrorWithThrowable
    }
}
