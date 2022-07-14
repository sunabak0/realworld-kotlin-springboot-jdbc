package com.example.realworldkotlinspringbootjdbc.domain

import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import java.util.Date
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body as CommentBody

class Comment private constructor(
    val id: CommentId,
    val body: CommentBody,
    val createdAt: Date,
    val updatedAt: Date,
    val author: OtherUser
) {
    companion object {
        /**
         * Validation 無し
         */
        fun newWithoutValidation(
            id: CommentId,
            body: CommentBody,
            createdAt: Date,
            updatedAt: Date,
            author: OtherUser
        ): Comment =
            Comment(id, body, createdAt, updatedAt, author)
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
