package com.example.realworldkotlinspringbootjdbc.usecase.comment

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.util.MyError

data class CommentWithAuthor(
    val comment: Comment,
    val author: OtherUser
)

interface CommentWithAuthorsQueryModel {
    fun fetchList(comments: List<Comment>): Either<FetchListError, List<CommentWithAuthor>> =
        throw NotImplementedError()

    sealed interface FetchListError : MyError
}
