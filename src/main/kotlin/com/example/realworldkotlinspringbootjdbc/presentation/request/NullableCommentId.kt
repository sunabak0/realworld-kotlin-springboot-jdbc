package com.example.realworldkotlinspringbootjdbc.presentation.request

/**
 * NullableCommentId
 *
 * 用途
 * - コメント削除
 *
 * 概要
 * - パスパラメータから一時的に String 型で受け取った CommentId を otIntOrNull 変換する
 *
 * 利用例
 * ```
 * val commentId = NullableCommentId.from("1") -> 1
 * val commentId = NullableCommentId.from("a") -> null
 * ```
 */
data class NullableCommentId(
    val id: String?
) {
    companion object {
        fun from(id: String?): Int? = id?.toIntOrNull()
    }
}
