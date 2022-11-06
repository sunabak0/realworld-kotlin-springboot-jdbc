package com.example.realworldkotlinspringbootjdbc.usecase.comment

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.util.MyError

data class CommentWithAuthor(
    val comment: Comment,
    val author: OtherUser
)

interface CommentWithAuthorsQueryModel {
    fun fetchList(
        comments: List<Comment>,
        currentUser: Option<RegisteredUser> = None
    ): Either<FetchListError, List<CommentWithAuthor>> =
        throw NotImplementedError()

    sealed interface FetchListError : MyError
}
