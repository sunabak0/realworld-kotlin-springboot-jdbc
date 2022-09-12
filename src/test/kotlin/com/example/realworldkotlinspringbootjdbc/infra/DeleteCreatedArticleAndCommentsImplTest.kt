package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.CommentRepository
import com.example.realworldkotlinspringbootjdbc.domain.DeleteCreatedArticleAndComments
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Primary
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Primary
class ThrowDataAccessExceptionCommentRepositoryImpl : CommentRepository {
    @Transactional
    override fun deleteAll(articleId: ArticleId): Either.Right<Unit> =
        throw object : DataAccessException("CommentRepository.deleteAll時に、例外が投げられる") {}
}

@SpringBootTest
@Tag("WithLocalDb")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DBRider
class DeleteCreatedArticleAndCommentsImplTest {
    @BeforeEach
    fun reset() = DbConnection.resetSequence()

    @Test
    @DataSet(
        value = [
            "datasets/yml/given/articles.yml",
            "datasets/yml/given/tags.yml",
        ],
    )
    @ExpectedDataSet(
        value = ["datasets/yml/then/delete_created_article_and_comments/execute-success-case-of-simple.yml"],
        ignoreCols = ["id", "created_at", "updated_at"],
        orderBy = ["id"]
    )
    // NOTE: @ExportDataSetはgivenの@DataSetが変更された時用に残しておく
    // @ExportDataSet(
    //    format = DataSetFormat.YML,
    //    outputName = "src/test/resources/datasets/yml/then/delete_created_article_and_comments/execute-success-case-of-simple.yml",
    //    includeTables = ["articles", "tags", "article_tags", "favorites", "article_comments"]
    // )
    fun `正常系-存在する作成済み記事を削除した場合、紐づくタグ・お気に入り・コメント類の関連も削除されるが、タグそのものは削除されない`() {
        /**
         * given:
         */
        val deleteCreatedArticleAndComments = DeleteCreatedArticleAndCommentsImpl(
            articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate),
            commentRepository = CommentRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
        )
        val existedArticleId = ArticleId(1)

        /**
         * when:
         */
        val actual = deleteCreatedArticleAndComments.execute(existedArticleId)

        /**
         * then:
         */
        val expected = Unit.right()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    @DataSet(
        value = [
            "datasets/yml/given/articles.yml",
            "datasets/yml/given/tags.yml",
        ],
    )
    @ExpectedDataSet(
        value = ["datasets/yml/then/delete_created_article_and_comments/execute-failed-case-of-not-existed-article-id.yml"],
        ignoreCols = ["id", "created_at", "updated_at"],
        orderBy = ["id"]
    )
    // NOTE: @ExportDataSetはgivenの@DataSetが変更された時用に残しておく
    // @ExportDataSet(
    //     format = DataSetFormat.YML,
    //     outputName = "src/test/resources/datasets/yml/then/delete_created_article_and_comments/execute-failed-case-of-not-existed-article-id.yml",
    //     includeTables = ["articles", "tags", "article_tags", "favorites", "article_comments"]
    // )
    fun `準正常系-存在しない作成済み記事を削除しようとした場合、存在しない旨のエラーが戻り値`() {
        /**
         * given:
         * - 存在しない作成済み記事Id
         */
        val deleteCreatedArticleAndComments = DeleteCreatedArticleAndCommentsImpl(
            articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate),
            commentRepository = CommentRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
        )
        val existedArticleId = ArticleId(99999)

        /**
         * when:
         */
        val actual = deleteCreatedArticleAndComments.execute(existedArticleId)

        /**
         * then:
         */
        val expected = DeleteCreatedArticleAndComments.Error.NotFoundArticle(articleId = ArticleId(99999)).left()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    @DataSet(
        value = [
            "datasets/yml/given/articles.yml",
            "datasets/yml/given/tags.yml",
        ],
    )
    @ExpectedDataSet(
        value = ["datasets/yml/then/delete_created_article_and_comments/execute-rollback-case-of-throw-data-access-exception.yml"],
        ignoreCols = ["id", "created_at", "updated_at"],
        orderBy = ["id"]
    )
    // NOTE: @ExportDataSetはgivenの@DataSetが変更された時用に残しておく
    // @ExportDataSet(
    //     format = DataSetFormat.YML,
    //     outputName = "src/test/resources/datasets/yml/then/delete_created_article_and_comments/execute-rollback-case-of-throw-data-access-exception.yml",
    //     includeTables = ["articles", "tags", "article_tags", "favorites", "article_comments"]
    // )
    fun `異常系-Commentの削除時にDBに関する例外が投げられた時、作成済み記事の削除もRollbackされて、なかったことになる`(
        @Autowired deleteCreatedArticleAndComments: DeleteCreatedArticleAndComments
    ) {
        /**
         * given:
         * - 存在する作成済み記事Id
         */
        val existedArticleId = ArticleId(1)

        /**
         * when:
         * - コメント削除時に例外が起きる
         */
        val actual = assertThrows<DataAccessException> {
            deleteCreatedArticleAndComments.execute(existedArticleId)
        }.message!!

        /**
         * then:
         */
        val expected = "CommentRepository.deleteAll時に、例外が投げられる"
        assertThat(actual).isEqualTo(expected)
    }
}
