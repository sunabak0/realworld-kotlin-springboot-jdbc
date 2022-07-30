package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface CommentRepository {
    fun list(slug: Slug): Either<ListError, List<Comment>> = TODO()
    sealed interface ListError : MyError {
        data class NotFoundArticleBySlug(val slug: Slug) : ListError, MyError.Basic
        data class Unexpected(override val cause: Throwable, val slug: Slug) : ListError, MyError.MyErrorWithThrowable
    }

    fun create(slug: Slug, body: Body, currentUserId: UserId): Either<CreateError, Comment> = TODO()
    sealed interface CreateError : MyError {
        data class NotFoundArticleBySlug(val slug: Slug) : CreateError, MyError.Basic
        data class Unexpected(
            override val cause: Throwable,
            val slug: Slug,
            val body: Body,
            val currentUserId: UserId
        ) : CreateError, MyError.MyErrorWithThrowable
    }

    fun delete(slug: Slug, commentId: CommentId, currentUserId: UserId): Either<DeleteError, Unit> = TODO()
    sealed interface DeleteError : MyError {
        data class ArticleNotFoundBySlug(val slug: Slug, val commentId: CommentId, val currentUserId: UserId) :
            DeleteError,
            MyError.Basic

        data class CommentNotFoundByCommentId(val slug: Slug, val commentId: CommentId, val currentUserId: UserId) :
            DeleteError, MyError.Basic

        data class DeleteCommentNotAuthorized(val slug: Slug, val commentId: CommentId, val currentUserId: UserId) :
            DeleteError, MyError.Basic

        data class Unexpected(
            override val cause: Throwable,
            val slug: Slug,
            val commentId: CommentId,
            val currentUserId: UserId
        ) : DeleteError, MyError.MyErrorWithThrowable
    }
}
