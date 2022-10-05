package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface CommentRepository {
    fun list(slug: Slug): Either<ListError, List<Comment>> = throw NotImplementedError()
    sealed interface ListError : MyError {
        data class NotFoundArticleBySlug(val slug: Slug) : ListError, MyError.Basic
        data class Unexpected(override val cause: Throwable, val slug: Slug) : ListError, MyError.MyErrorWithThrowable
    }

    fun create(slug: Slug, body: Body, currentUserId: UserId): Either<CreateError, Comment> = throw NotImplementedError()
    sealed interface CreateError : MyError {
        data class NotFoundArticleBySlug(val slug: Slug) : CreateError, MyError.Basic
        data class Unexpected(
            override val cause: Throwable,
            val slug: Slug,
            val body: Body,
            val currentUserId: UserId
        ) : CreateError, MyError.MyErrorWithThrowable
    }

    fun delete(slug: Slug, commentId: CommentId, currentUserId: UserId): Either<DeleteError, Unit> = throw NotImplementedError()
    sealed interface DeleteError : MyError {
        data class NotFoundArticleBySlug(val slug: Slug, val commentId: CommentId, val currentUserId: UserId) :
            DeleteError,
            MyError.Basic

        data class NotFoundCommentByCommentId(val slug: Slug, val commentId: CommentId, val currentUserId: UserId) :
            DeleteError, MyError.Basic

        data class NotAuthorizedDeleteComment(val slug: Slug, val commentId: CommentId, val currentUserId: UserId) :
            DeleteError, MyError.Basic

        data class Unexpected(
            override val cause: Throwable,
            val slug: Slug,
            val commentId: CommentId,
            val currentUserId: UserId
        ) : DeleteError, MyError.MyErrorWithThrowable
    }

    /**
     * 作成済み記事に紐づくコメントを全て削除
     *
     * 注) 削除するコメントがない -> 作成済み記事が存在しないとは限らない
     *
     * @param articleId 削除したい作成済み記事のId
     * @return Unit
     */
    fun deleteAll(articleId: ArticleId): Either.Right<Unit> = throw NotImplementedError()
}
