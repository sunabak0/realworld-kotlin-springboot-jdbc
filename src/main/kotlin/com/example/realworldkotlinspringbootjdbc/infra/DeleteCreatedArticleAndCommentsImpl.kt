package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.CommentRepository
import com.example.realworldkotlinspringbootjdbc.domain.DeleteCreatedArticleAndComments
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Primary
class DeleteCreatedArticleAndCommentsImpl(
    val articleRepository: ArticleRepository,
    val commentRepository: CommentRepository,
) : DeleteCreatedArticleAndComments {
    /**
     * ネストした @Transactional
     * 2つのリポジトリで、どちらかがDB周りの例外が投げられたら 両方rollback
     */
    @Transactional
    override fun execute(articleId: ArticleId): Either<DeleteCreatedArticleAndComments.Error, Unit> =
        when (val deleteArticleResult = articleRepository.delete(articleId)) {
            /**
             * 作成済み記事削除: 失敗
             */
            is Left -> when (deleteArticleResult.value) {
                /**
                 * 原因: 作成済み記事が見つからなかった
                 */
                is ArticleRepository.DeleteError.NotFoundArticle ->
                    DeleteCreatedArticleAndComments.Error.NotFoundArticle(articleId).left()
            }
            /**
             * 作成済み記事削除: 成功
             */
            is Right -> commentRepository.deleteAll(articleId)
        }
}
