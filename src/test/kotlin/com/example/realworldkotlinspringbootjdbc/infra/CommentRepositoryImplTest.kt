package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either.Left
import arrow.core.Either.Right
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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.text.SimpleDateFormat

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("WithLocalDb")
class CommentRepositoryImplTest {
    companion object {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate

        fun resetSequence() {
            val sql = """
                SELECT
                    setval('articles_id_seq', 10000)
                    , setval('tags_id_seq', 10000)
                ;
            """.trimIndent()
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
                "datasets/yml/given/articles.yml",
                "datasets/yml/given/article-comments.yml"
            ],
        )
        fun `正常系-articles テーブルに slug に該当する記事が存在した場合、Comment の List が戻り値`() {
            // given:
            val commentRepository = CommentRepositoryImpl(namedParameterJdbcTemplate)

            // when:
            val actual = commentRepository.list(Slug.newWithoutValidation("dummy-slug"))

            // then:
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val expected = listOf(
                Comment.newWithoutValidation(
                    CommentId.newWithoutValidation(1),
                    Body.newWithoutValidation("dummy=body-1"),
                    date,
                    date,
                    UserId(1),
                ),
                Comment.newWithoutValidation(
                    CommentId.newWithoutValidation(2),
                    Body.newWithoutValidation("dummy-body-2"),
                    date,
                    date,
                    UserId(2),
                ),
            )
            when (actual) {
                is Left -> assert(false)
                is Right -> assertThat(actual.value).isEqualTo(expected)
            }
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/articles.yml",
                "datasets/yml/given/empty-article-comments.yml"
            ],
        )
        fun `正常系-articles テーブルに slug に該当するが Comment 存在しなかった場合、空の Comment の List が戻り値`() {
            // given:
            val commentRepository = CommentRepositoryImpl(namedParameterJdbcTemplate)

            // when:
            val actual = commentRepository.list(Slug.newWithoutValidation("dummy-slug"))

            // then:
            val expected = listOf<Comment>()
            when (actual) {
                is Left -> assert(false)
                is Right -> assertThat(actual.value).isEqualTo(expected)
            }
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/empty-articles.yml",
            ],
        )
        fun `異常系-articles テーブルに slug に該当する記事がなかった場合、NotFoundError が戻り値`() {
            // given:
            val commentRepository = CommentRepositoryImpl(namedParameterJdbcTemplate)

            // when:
            val actual = commentRepository.list(Slug.newWithoutValidation("dummy-slug"))

            // then:
            val expected = CommentRepository.ListError.NotFoundArticleBySlug(Slug.newWithoutValidation("dummy-slug"))
            when (actual) {
                is Left -> assertThat(actual.value).isEqualTo(expected)
                is Right -> assert(false)
            }
        }

        @Test
        @Disabled
        fun `異常系-UnexpectedError が戻り値`() {
            TODO()
        }
    }
}
