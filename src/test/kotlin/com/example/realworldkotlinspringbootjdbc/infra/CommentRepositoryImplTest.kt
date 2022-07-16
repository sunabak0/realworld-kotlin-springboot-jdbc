package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.CommentRepository
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import java.text.SimpleDateFormat

class CommentRepositoryImplTest {
    companion object {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate

        fun resetSequence() {
            val sql = """
                SELECT
                    setval('articles_id_seq', 10000)
                    , setval('tags_id_seq', 10000)
                    , setval('article_comments_id_seq', 10000)
                ;
            """.trimIndent()
            DbConnection.namedParameterJdbcTemplate.queryForRowSet(sql, MapSqlParameterSource())
        }
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("List(コメント一覧を表示)")
    class List {
        @BeforeAll
        fun reset() = resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/articles.yml"
            ],
        )
        fun `正常系-articles テーブルに slug に該当する作成済記事（CreatedArticle）が存在した場合、コメント（Comment） の List が戻り値`() {
            /**
             * given
             */
            val commentRepository = CommentRepositoryImpl(namedParameterJdbcTemplate)

            /**
             * when
             */
            val actual = commentRepository.list(Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"))

            /**
             * then
             */
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val expected = listOf(
                Comment.newWithoutValidation(
                    id = CommentId.newWithoutValidation(1),
                    body = Body.newWithoutValidation("dummy-comment-body-01"),
                    createdAt = date,
                    updatedAt = date,
                    authorId = UserId(3),
                ),
                Comment.newWithoutValidation(
                    id = CommentId.newWithoutValidation(3),
                    body = Body.newWithoutValidation("dummy-comment-body-03"),
                    createdAt = date,
                    updatedAt = date,
                    authorId = UserId(2),
                ),
                Comment.newWithoutValidation(
                    id = CommentId.newWithoutValidation(5),
                    body = Body.newWithoutValidation("dummy-comment-body-02"),
                    createdAt = date,
                    updatedAt = date,
                    authorId = UserId(3),
                ),
            )
            when (actual) {
                is Left -> assert(false)
                is Right -> {
                    // List<Comment> 1 つめの比較（createdAt, updateAtはメタデータなので比較しない）
                    assertThat(actual.value[0].id).isEqualTo(expected[0].id)
                    assertThat(actual.value[0].body).isEqualTo(expected[0].body)
                    assertThat(actual.value[0].authorId).isEqualTo(expected[0].authorId)
                    // List<Comment> 2 つめの比較（createdAt, updateAtはメタデータなので比較しない）
                    assertThat(actual.value[1].id).isEqualTo(expected[1].id)
                    assertThat(actual.value[1].body).isEqualTo(expected[1].body)
                    assertThat(actual.value[1].authorId).isEqualTo(expected[1].authorId)
                    // List<Comment> 3 つめの比較（createdAt, updateAtはメタデータなので比較しない）
                    assertThat(actual.value[2].id).isEqualTo(expected[2].id)
                    assertThat(actual.value[2].body).isEqualTo(expected[2].body)
                    assertThat(actual.value[2].authorId).isEqualTo(expected[2].authorId)
                }
            }
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/articles.yml",
            ],
        )
        fun `正常系-articles テーブルに slug に該当するが Comment 存在しなかった場合、空の Comment の List が戻り値`() {
            /**
             * given:
             */
            val commentRepository = CommentRepositoryImpl(namedParameterJdbcTemplate)

            /**
             * when:
             */
            val actual = commentRepository.list(Slug.newWithoutValidation("functional-programming-kotlin"))

            /**
             * then:
             */
            val expected = listOf<Comment>().right()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/empty-articles.yml",
            ],
        )
        fun `準正常系-articles テーブルに slug に該当する記事がなかった場合、NotFoundError が戻り値`() {
            /**
             * given:
             */
            val commentRepository = CommentRepositoryImpl(namedParameterJdbcTemplate)

            /**
             * when:
             */
            val actual = commentRepository.list(Slug.newWithoutValidation("not-found-article-slug"))

            /**
             * then:
             */
            val expected =
                CommentRepository.ListError.NotFoundArticleBySlug(Slug.newWithoutValidation("not-found-article-slug"))
                    .left()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class `Create（コメントを作成）` {
        @BeforeEach
        @AfterAll
        fun reset() {
            resetDb()
        }

        @Test
        fun `正常系-articles テーブルに slug に該当する記事が存在し、comments テーブルに挿入できた場合、戻り値が Comments`() {
            fun localPrepare() {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
                val insertArticleSql = """
                    INSERT INTO
                        articles (
                            id
                            , author_id
                            , title
                            , slug
                            , body
                            , description
                            , created_at
                            , updated_at
                        )
                    VALUES (
                        :id
                        , :author_id
                        , :title
                        , :slug
                        , :body
                        , :description
                        , :created_at
                        , :updated_at
                    );
                """.trimIndent()
                val insertArticleSqlParams = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("author_id", 1)
                    .addValue("title", "dummy-title")
                    .addValue("slug", "dummy-slug")
                    .addValue("body", "dummy-body")
                    .addValue("description", "dummy-description")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertArticleSql, insertArticleSqlParams)
            }
            localPrepare()
            /**
             * 実行前に挿入されていないことを確認
             */
            val confirmCommentsSql = "SELECT COUNT(*) AS CNT FROM article_comments;"
            val confirmCommentsSqlParam = MapSqlParameterSource()
            val beforeCommentsCount = namedParameterJdbcTemplate.queryForMap(confirmCommentsSql, confirmCommentsSqlParam)["CNT"]
            assertThat(beforeCommentsCount).isEqualTo(0L)

            val commentRepository = CommentRepositoryImpl(namedParameterJdbcTemplate)

            /**
             * TODO: 戻り値（Comment）を期待値（expected）と比較するのか要検討。CommentId を DB のオートインクリメントにしているので期待値を予測できない。他のプロパティで比較するか、CommentId を他の方法で生成するか検討する
             */
            when (commentRepository.create(Slug.newWithoutValidation("dummy-slug"), Body.newWithoutValidation("dummy-body-1"), UserId(1))) {
                is Left -> assert(false)
                is Right -> assert(true)
            }

            /**
             * 実行後に1行だけ挿入されていることを確認
             */
            val afterCommentsCount = namedParameterJdbcTemplate.queryForMap(confirmCommentsSql, confirmCommentsSqlParam)["CNT"]
            assertThat(afterCommentsCount).isEqualTo(1L)
        }

        @Test
        fun `異常系-articles テーブルに slug に該当する記事が存在しない場合、戻り値が NotFoundError`() {
            /**
             * 実行前に挿入されていないことを確認
             */
            val confirmCommentsSql = "SELECT COUNT(*) AS CNT FROM article_comments;"
            val confirmCommentsSqlParam = MapSqlParameterSource()
            val beforeCommentsCount = namedParameterJdbcTemplate.queryForMap(confirmCommentsSql, confirmCommentsSqlParam)["CNT"]
            assertThat(beforeCommentsCount).isEqualTo(0L)

            val commentRepository = CommentRepositoryImpl(namedParameterJdbcTemplate)
            val expected = CommentRepository.CreateError.NotFoundArticleBySlug(Slug.newWithoutValidation("dummy-slug"))

            when (val actual = commentRepository.create(Slug.newWithoutValidation("dummy-slug"), Body.newWithoutValidation("dummy-body-1"), UserId(1))) {
                is Left -> assertThat(actual.value).isEqualTo(expected)
                is Right -> assert(false)
            }
        }

        @Test
        @Disabled
        fun `異常系-原因不明のため UnexpectedError が戻り値`() {
            TODO()
        }
    }
}
