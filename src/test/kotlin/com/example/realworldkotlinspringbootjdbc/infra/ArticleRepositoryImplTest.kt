package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.right
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag as ArticleTag
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
                DELETE FROM article_tags;
                DELETE FROM tags;
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
        fun `異常系 articles テーブルに slug 該当する記事が存在し、findBySlug を実行したとき、Article を戻す`() {
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

                val insertArticleTagsSql = """
                    INSERT INTO
                        article_tags (
                            id
                            , article_id
                            , tag_id
                            , created_at
                        )
                    VALUES (
                        :id
                        , :article_id
                        , :tag_id
                        , :created_at
                    )
                    ;
                """.trimIndent()
                val insertArticleTagsSqlParams1 = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("article_id", 1)
                    .addValue("tag_id", 1)
                    .addValue("created_at", date)
                namedParameterJdbcTemplate.update(insertArticleTagsSql, insertArticleTagsSqlParams1)
                val insertArticleTagsSqlParams2 = MapSqlParameterSource()
                    .addValue("id", 2)
                    .addValue("article_id", 1)
                    .addValue("tag_id", 2)
                    .addValue("created_at", date)
                namedParameterJdbcTemplate.update(insertArticleTagsSql, insertArticleTagsSqlParams2)

                val insertTagsSql = """
                    INSERT INTO
                        tags (
                            id
                            , name
                            , created_at
                            , updated_at
                        )
                    VALUES (
                        :id
                        , :name
                        , :created_at
                        , :updated_at
                    )
                """.trimIndent()
                val insertTagsSqlMap1 = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("name", "dummy-tag-1")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertTagsSql, insertTagsSqlMap1)
                val insertTagsSqlMap2 = MapSqlParameterSource()
                    .addValue("id", 2)
                    .addValue("name", "dummy-tag-2")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertTagsSql, insertTagsSqlMap2)
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
                listOf(ArticleTag.newWithoutValidation("dummy-tag-1"), ArticleTag.newWithoutValidation("dummy-tag-2")),
                UserId(1),
                favorited = false,
                favoritesCount = 0
            )

            when (val actual = repository.findBySlug(Slug.newWithoutValidation("dummy-slug"))) {
                is Left -> assert(false)
                is Right -> {
                    /**
                     * エンティティの識別子による同一性の確認
                     */
                    assertThat(actual.value).isEqualTo(expected)
                    /**
                     * プロパティが期待値と全て等しいのか確認
                     */
                    assertThat(actual.value.id).isEqualTo(expected.id)
                    assertThat(actual.value.title).isEqualTo(expected.title)
                    assertThat(actual.value.slug).isEqualTo(expected.slug)
                    assertThat(actual.value.body).isEqualTo(expected.body)
                    assertThat(actual.value.createdAt).isEqualTo(expected.createdAt)
                    assertThat(actual.value.updatedAt).isEqualTo(expected.updatedAt)
                    assertThat(actual.value.description).isEqualTo(expected.description)
                    assertThat(actual.value.tagList).isEqualTo(expected.tagList)
                    assertThat(actual.value.authorId).isEqualTo(expected.authorId)
                    assertThat(actual.value.favorited).isEqualTo(expected.favorited)
                    assertThat(actual.value.favoritesCount).isEqualTo(expected.favoritesCount)
                }
            }
        }

        @Test
        fun `異常系 articles テーブルに slug に該当する記事が存在せずに、findBySlug を実行したとき、NotFound を戻す`() {
            val repository = ArticleRepositoryImpl(namedParameterJdbcTemplate)

            val expected = ArticleRepository.FindBySlugError.NotFound(
                Slug.newWithoutValidation("dummy-slug")
            )
            when (val actual = repository.findBySlug(Slug.newWithoutValidation("dummy-slug"))) {
                is Left -> assertThat(actual.value).isEqualTo(expected)
                is Right -> assertThat(false)
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
