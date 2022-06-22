package com.example.realworldkotlinspringbootjdbc.domain

import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body as CommentBody
import java.util.Date

interface Comment {
    val id: CommentId
    val body: CommentBody
    val createdAt: Date
    val updatedAt: Date
    val author: Profile

    /**
     * 実装
     */
    private data class CommentWithoutValidation(
        override val id: CommentId,
        override val body: CommentBody,
        override val createdAt: Date,
        override val updatedAt: Date,
        override val author: Profile
    ) : Comment

    /**
     * Factory メソッド
     */
    companion object {
        /**
         * Validation 無し
         */
        fun newWithoutValidation(id: CommentId, body: CommentBody, createdAt: Date, updatedAt: Date, author: Profile): Comment =
            CommentWithoutValidation(id, body, createdAt, updatedAt, author)
    }
}
