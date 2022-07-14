package com.example.realworldkotlinspringbootjdbc.domain

import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import java.util.Date
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body as CommentBody

class Comment private constructor(
    val id: CommentId,
    val body: CommentBody,
    val createdAt: Date,
    val updatedAt: Date,
    val authorId: UserId
) {
    companion object {
        fun newWithoutValidation(
            id: CommentId,
            body: CommentBody,
            createdAt: Date,
            updatedAt: Date,
            authorId: UserId
        ): Comment =
            Comment(id, body, createdAt, updatedAt, authorId)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Comment
        return this.id == other.id
    }

    override fun hashCode(): Int {
        return id.value * 31
    }
}
