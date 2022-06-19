package com.example.realworldkotlinspringbootjdbc.domain

import java.util.Date

interface Comment {
    val id: Int
    val body: String
    val createdAt: Date
    val updatedAt: Date
    val author: String

    /**
     * 実装
     */
    private data class CommentWithoutValidation(
        override val id: Int,
        override val body: String,
        override val createdAt: Date,
        override val updatedAt: Date,
        override val author: String
    ) : Comment

    /**
     * Factory メソッド
     */
    companion object {
        /**
         * Validation 無し
         */
        fun newWithoutValidation(id: Int, body: String, createdAt: Date, updatedAt: Date, author: String): Comment =
            Comment.CommentWithoutValidation(id, body, createdAt, updatedAt, author)
    }
}
