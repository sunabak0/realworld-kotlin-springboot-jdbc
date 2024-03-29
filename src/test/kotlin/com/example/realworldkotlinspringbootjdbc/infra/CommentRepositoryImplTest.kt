package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.CommentRepository
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.text.SimpleDateFormat

class CommentRepositoryImplTest {
    companion object {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("List(コメント一覧を表示)")
    class List {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

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

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("Create（コメントを作成）")
    class Create {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet("datasets/yml/given/articles.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/then/comment_repository/create-success.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `正常系-articles テーブルに slug に該当する作成済記事が存在し、comments テーブルに挿入できた場合、コメント（Comment）が戻り値`() {
            /**
             * given:
             */
            val commentRepository = CommentRepositoryImpl(namedParameterJdbcTemplate)

            /**
             * when:
             */
            val actual = commentRepository.create(
                Slug.newWithoutValidation("functional-programming-kotlin"),
                Body.newWithoutValidation("created-dummy-body-1"),
                UserId(1)
            )

            /**
             * then: CommentRepository.create の戻り値との比較
             */
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val expected = Comment.newWithoutValidation(
                id = CommentId.newWithoutValidation(10001), // 比較しない
                body = Body.newWithoutValidation("created-dummy-body-1"),
                createdAt = date, // 比較しない
                updatedAt = date, // 比較しない
                authorId = UserId(1)
            )

            when (actual) {
                is Left -> assert(false)
                is Right -> {
                    // id は DB に採番されるため比較しない。 createdAt、updatedAt は メタデータのため比較しない
                    assertThat(actual.value.body).isEqualTo(expected.body)
                    assertThat(actual.value.authorId).isEqualTo(expected.authorId)
                }
            }
        }

        @Test
        @DataSet("datasets/yml/given/articles.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/given/articles.yml"],
            orderBy = ["id"],
            ignoreCols = ["id", "created_at", "updated_at"]
        )
        fun `準正常系-articles テーブルに slug に該当する記事が存在しない場合、NotFoundError が戻り値`() {
            /**
             * given:
             */
            val commentRepository = CommentRepositoryImpl(namedParameterJdbcTemplate)

            /**
             * when:
             */
            val actual = commentRepository.create(
                Slug.newWithoutValidation("dummy-slug"),
                Body.newWithoutValidation("dummy-body-1"),
                UserId(1)
            )

            /**
             * then:
             */
            val expected = CommentRepository.CreateError.NotFoundArticleBySlug(Slug.newWithoutValidation("dummy-slug"))
            when (actual) {
                is Left -> assertThat(actual.value).isEqualTo(expected)
                is Right -> assert(false)
            }
        }
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("Delete（コメントを削除）")
    class Delete {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet("datasets/yml/given/articles.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/then/comment_repository/delete-success.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `正常系-「articles テーブルに slug に該当する記事が存在」「comments テーブルに commentId に該当するコメントが存在」「コメントの authorId が currentUserId と同じ」場合、削除成功し、戻り値が Unit`() {
            /**
             * given:
             */
            val commentRepository = CommentRepositoryImpl(namedParameterJdbcTemplate)

            /**
             * when:
             * - slug: articles テーブルに存在する
             * - commentId: article_comments テーブルに存在する
             * - currentUserId: article_comments テーブルの authorId と同じ
             */
            val actual = commentRepository.delete(
                slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                commentId = CommentId.newWithoutValidation(1),
                currentUserId = UserId(3)
            )

            /**
             * then:
             */
            assertThat(actual.isRight()).isTrue
        }

        @Test
        @DataSet("datasets/yml/given/articles.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/given/articles.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `準正常系-「articles テーブルに slug に該当する作成済記事が存在しない」場合、削除失敗し、戻り値が ArticleNotFoundBySlug`() {
            /**
             * given:
             */
            val commentRepository = CommentRepositoryImpl(namedParameterJdbcTemplate)

            /**
             * when:
             * - slug: articles テーブルに存在しない
             * - commentId: このテストで値に意味はない
             * - currentUserId: このテストで値に意味はない
             */
            val actual = commentRepository.delete(
                slug = Slug.newWithoutValidation("dummy-not-exist-slug"), // 存在しない作成済記事
                commentId = CommentId.newWithoutValidation(1),
                currentUserId = UserId(3)
            )

            /**
             * then:
             */
            val expected = CommentRepository.DeleteError.NotFoundArticleBySlug(
                slug = Slug.newWithoutValidation("dummy-not-exist-slug"),
                commentId = CommentId.newWithoutValidation(1),
                currentUserId = UserId(3)
            ).left()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet("datasets/yml/given/articles.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/given/articles.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `準正常系-「articles テーブルに slug に該当する記事が存在」「comments テーブルに commentId に該当するコメントが存在しない」場合、削除失敗し、戻り値が CommentNotFoundByCommentId`() {
            /**
             * given:
             */
            val commentRepository = CommentRepositoryImpl(namedParameterJdbcTemplate)

            /**
             * when:
             * - slug: articles テーブルに存在する
             * - commentId: article_comments テーブルに存在しない
             * - currentUserId: このテストで値に意味がない
             */
            val actual = commentRepository.delete(
                slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                commentId = CommentId.newWithoutValidation(100), // 存在しない CommentId
                currentUserId = UserId(3)
            )

            /**
             * then:
             */
            val expected = CommentRepository.DeleteError.NotFoundCommentByCommentId(
                slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                commentId = CommentId.newWithoutValidation(100),
                currentUserId = UserId(3)
            ).left()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet("datasets/yml/given/articles.yml")
        @ExpectedDataSet(
            value = ["datasets/yml/given/articles.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `準正常系-「articles テーブルに slug に該当する記事が存在」「comments テーブルに commentId に該当するコメントが存在」「コメントの authorId が currentUserId と異なる」場合、削除失敗し、戻り値が NotAuthorizedDeleteComment`() {
            /**
             * given:
             */
            val commentRepository = CommentRepositoryImpl(namedParameterJdbcTemplate)

            /**
             * when:
             * - slug: articles テーブルに存在する
             * - commentId: article_comments テーブルに存在する
             * - currentUserId: commentId に該当する article_comments テーブルのレコードの authorId と一致しない
             */
            val actual = commentRepository.delete(
                slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                commentId = CommentId.newWithoutValidation(1),
                currentUserId = UserId(100) // コメントの authorId と 一致しない
            )

            /**
             * then:
             */
            val expected = CommentRepository.DeleteError.NotAuthorizedDeleteComment(
                slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                commentId = CommentId.newWithoutValidation(1),
                currentUserId = UserId(100)
            ).left()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("DeleteAll（特定の作成済み記事のコメントを全て削除）")
    class DeleteAll {
        @Test
        @DataSet(
            value = [
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        @ExpectedDataSet(
            value = ["datasets/yml/then/comment_repository/delete-all-success-case-of-simple.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        // NOTE: @ExportDataSetはgivenの@DataSetが変更された時用に残しておく
        // @ExportDataSet(
        //    format = DataSetFormat.YML,
        //    outputName = "src/test/resources/datasets/yml/then/comment_repository/delete-all-success-case-of-simple.yml",
        //    includeTables = ["articles", "article_comments"]
        // )
        fun `正常系-作成済み記事に紐づくコメントが全て削除される`() {
            /**
             * given:
             * - コメントが複数紐付いている作成済み記事Id
             */
            val commentRepository = CommentRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val targetArticleId = ArticleId(1)

            /**
             * when:
             */
            val actual = commentRepository.deleteAll(targetArticleId)

            /**
             * then:
             */
            val expected = Right(Unit)
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        @ExpectedDataSet(
            value = ["datasets/yml/then/comment_repository/delete-all-success-case-of-no-comments.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        // NOTE: @ExportDataSetはgivenの@DataSetが変更された時用に残しておく
        // @ExportDataSet(
        //    format = DataSetFormat.YML,
        //    outputName = "src/test/resources/datasets/yml/then/comment_repository/delete-all-success-case-of-no-comments.yml",
        //    includeTables = ["articles", "article_comments"]
        // )
        fun `正常系-作成済み記事に紐づくコメントが1つもなくても、成功した旨が戻り値`() {
            /**
             * given:
             * - 存在はするが、紐付くコメントが1つも無い作成済み記事Id
             */
            val commentRepository = CommentRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val targetArticleId = ArticleId(2)

            /**
             * when:
             */
            val actual = commentRepository.deleteAll(targetArticleId)

            /**
             * then:
             */
            val expected = Right(Unit)
            assertThat(actual).isEqualTo(expected)
        }

        /**
         * ユースケース上はありえない
         * 技術詳細(引数)的には可能なためテストを記述
         */
        @Test
        @DataSet(
            value = [
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        @ExpectedDataSet(
            value = ["datasets/yml/then/comment_repository/delete-all-success-case-of-not-existed-article-id.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        // NOTE: @ExportDataSetはgivenの@DataSetが変更された時用に残しておく
        // @ExportDataSet(
        //    format = DataSetFormat.YML,
        //    outputName = "src/test/resources/datasets/yml/then/comment_repository/delete-all-success-case-of-not-existed-article-id.yml",
        //    includeTables = ["article_comments"]
        // )
        fun `仕様外-存在しない作成済み記事Idを指定しても、成功した旨が戻り値`() {
            /**
             * given:
             * - 存在しない作成済み記事Id
             */
            val commentRepository = CommentRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val targetArticleId = ArticleId(99999)

            /**
             * when:
             */
            val actual = commentRepository.deleteAll(targetArticleId)

            /**
             * then:
             */
            val expected = Right(Unit)
            assertThat(actual).isEqualTo(expected)
        }
    }
}
