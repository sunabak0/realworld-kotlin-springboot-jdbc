package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Validated
import arrow.core.invalid
import arrow.core.valid
import com.example.realworldkotlinspringbootjdbc.util.MyError

/**
 * ドメインサービス
 *
 * 作成済み記事の著者かどうかを検証する
 */
object CreatedArticleAuthorValidationDomainService {
    interface Error : MyError {
        data class NotMatchedUserId(val article: CreatedArticle, val user: RegisteredUser) : Error, MyError.Basic
    }

    /**
     * 作成済み記事の著者かどうかを検証する
     *
     * @param article
     * @param user
     * @return (著者ではない: Invalid) or (著者である: Valid)
     */
    fun validate(article: CreatedArticle, user: RegisteredUser): Validated<Error, Unit> =
        when (article.authorId == user.userId) {
            false -> Error.NotMatchedUserId(article, user).invalid()
            true -> Unit.valid()
        }
}
