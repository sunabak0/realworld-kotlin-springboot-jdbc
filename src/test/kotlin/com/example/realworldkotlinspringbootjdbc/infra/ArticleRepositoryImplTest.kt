package com.example.realworldkotlinspringbootjdbc.infra

<<<<<<< HEAD
import arrow.core.right
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
=======
import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag as ArticleTag
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import java.text.SimpleDateFormat
import com.example.realworldkotlinspringbootjdbc.domain.article.Body as ArticleBody

class ArticleRepositoryImplTest {
    companion object {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
        fun resetDb() {
            val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
            val sql = """
                DELETE FROM articles;
            """.trimIndent()
            namedParameterJdbcTemplate.update(sql, MapSqlParameterSource())
        }
    }

    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Find() {
        @BeforeEach
        @AfterAll
        fun reset() {
            resetDb()
        }

        @Test
        fun `成功 articles テーブルに slug 該当する記事が存在する場合、Article を戻す`() {
            fun localPrepare() {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
                val insertArticlesSql = """
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
                    )
                """.trimIndent()
                val insertArticlesSqlParams = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("author_id", 1)
                    .addValue("title", "dummy-title")
                    .addValue("slug", "dummy-slug")
                    .addValue("body", "dummy-body")
                    .addValue("description", "dummy-description")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertArticlesSql, insertArticlesSqlParams)
            }
            localPrepare()
            val repository = ArticleRepositoryImpl(namedParameterJdbcTemplate)
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val expected = CreatedArticle.newWithoutValidation(
                ArticleId(1),
                Title.newWithoutValidation("dummy-title"),
                Slug.newWithoutValidation("dummy-slug"),
                ArticleBody.newWithoutValidation("dummy-body"),
                createdAt = date,
                updatedAt = date,
                Description.newWithoutValidation("dummy-description"),
                listOf(),
                UserId(1),
                favorited = false,
                favoritesCount = 0
            )

            when (val actual = repository.findBySlug(Slug.newWithoutValidation("dummy-slug"))) {
                is Either.Left -> assert(false)
                is Either.Right -> assertThat(actual.value).isEqualTo(expected)
            }
        }

        @Test
        fun `失敗 slug に該当する記事がなかった場合、FindBySlugErrorNotFound を戻す`() {
            val repository = ArticleRepositoryImpl(namedParameterJdbcTemplate)

            val expected = ArticleRepository.FindBySlugError.NotFound(
                Slug.newWithoutValidation("dummy-slug")
            )
            when (val actual = repository.findBySlug(Slug.newWithoutValidation("dummy-slug"))) {
                is Either.Left -> assertThat(actual.value).isEqualTo(expected)
                is Either.Right -> assertThat(false)
            }
        }

        @Test
        @Disabled
        fun `異常系 UnexpectedError が戻り値`() {
            TODO()
        }
    }

    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("タグ一覧")
    class TagsTest {
        @Test
        @DataSet("datasets/yml/given/tags.yml")
        fun `成功-タグ一覧取得に成功した場合、タグの一覧が戻り値となる`() {
            // given:
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)

            // when:
            val actual = articleRepository.tags()

            // then:
            val expected = listOf(
                ArticleTag.newWithoutValidation("rust"),
                ArticleTag.newWithoutValidation("scala"),
                ArticleTag.newWithoutValidation("kotlin"),
                ArticleTag.newWithoutValidation("ocaml"),
                ArticleTag.newWithoutValidation("elixir"),
            ).right()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet("datasets/yml/given/empty-tags.yml")
        fun `成功-tagsテーブルが空で、タグ一覧取得に成功した場合、空のタグ一覧が戻り値となる`() {
            // given:
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)

            // when:
            val actual = articleRepository.tags()

            // then:
            val expected = listOf<ArticleTag>().right()
            assertThat(actual).isEqualTo(expected)
        }
    }
}
