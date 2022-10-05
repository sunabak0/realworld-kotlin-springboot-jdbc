package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.util.MyError

/**
 * ドメインサービス
 *
 * - 作成済み記事を削除
 * - 関連づくコメントを削除
 */
interface DeleteCreatedArticleAndComments {
    /**
     * 実行
     *
     * @param articleId 削除したい作成済み記事のId
     * @return 準正常系: エラー or 正常系: Unit
     */
    fun execute(articleId: ArticleId): Either<Error, Unit> = throw NotImplementedError()

    sealed interface Error : MyError {
        data class NotFoundArticle(val articleId: ArticleId) : Error, MyError.Basic
    }
}
