package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.util.MyError

/**
 * ドメインサービス
 *
 * 作成済み記事の著者かどうかを検証する
 */
object CreatedArticleAuthorVerification {
    interface Error : MyError {
        data class NotMatchedUserId(val article: CreatedArticle, val user: RegisteredUser) : Error, MyError.Basic
    }

    /**
     * 作成済み記事の著者かどうかを検証する
     *
     * @param article
     * @param user
     * @return (著者ではない: Left) or (著者である: Right)
     */
    fun verify(article: CreatedArticle, user: RegisteredUser): Either<Error, Unit> =
        when (article.authorId == user.userId) {
            false -> Error.NotMatchedUserId(article, user).left()
            true -> Unit.right()
        }
}
