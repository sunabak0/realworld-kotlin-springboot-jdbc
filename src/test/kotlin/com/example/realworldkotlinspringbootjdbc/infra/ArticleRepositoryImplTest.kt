package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag as ArticleTag

class ArticleRepositoryImplTest {
    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("Slugで記事検索")
    class FindBySlugTest {
        @BeforeAll
        fun reset() = UserRepositoryImplTest.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `成功-slugに該当する作成済み記事がある場合、作成済み記事を返す`() {
            // given:
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val searchingSlug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin")

            // when:
            val actual = articleRepository.findBySlug(searchingSlug)

            // then:
            when (actual) {
                is Left -> assert(false)
                is Right -> {
                    val createdArticle = actual.value
                    assertThat(createdArticle.slug.value).isEqualTo(searchingSlug.value)
                }
            }
        }

        @Test
        @DataSet("datasets/yml/given/empty-articles.yml")
        fun `失敗-slugに該当する作成済み記事がない場合、NotFoundを返す`() {
            // given:
            val repository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val searchingSlug = Slug.newWithoutValidation("not-found-slug")

            // when:
            val actual = repository.findBySlug(searchingSlug)

            // then:
            val expected = ArticleRepository.FindBySlugError.NotFound(searchingSlug).left()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("Slugで(特定の登録済みユーザー観点の)作成済み記事の検索")
    class FindBySlugFromRegisteredUserViewpointTest {
        @BeforeAll
        fun reset() = UserRepositoryImplTest.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `正常系-Slug に該当する記事が存在し、SlugとUserIdで検索すると、お気に入り済みかどうかが反映された作成された記事を取得できる`() {
            // given:
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val searchingSlug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin")
            val favoritedArticleUserId = UserId(2)
            val unfavoritedArticleUserId = UserId(1)

            // when: お気に入りしている登録済みユーザー/お気に入りしていない登録済みユーザーでそれぞれ検索
            val favoritedArticle =
                articleRepository.findBySlugFromRegisteredUserViewpoint(searchingSlug, favoritedArticleUserId)
            val unfavoritedArticle =
                articleRepository.findBySlugFromRegisteredUserViewpoint(searchingSlug, unfavoritedArticleUserId)

            // then:
            when (favoritedArticle) {
                is Left -> assert(false)
                is Right -> {
                    val createdArticle = favoritedArticle.value
                    assertThat(createdArticle.slug.value).isEqualTo(searchingSlug.value)
                    assertThat(createdArticle.favorited).isTrue
                }
            }
            when (unfavoritedArticle) {
                is Left -> assert(false)
                is Right -> {
                    val createdArticle = unfavoritedArticle.value
                    assertThat(createdArticle.slug.value).isEqualTo(searchingSlug.value)
                    assertThat(createdArticle.favorited).isFalse
                }
            }
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/empty-articles.yml"
            ]
        )
        fun `準正常系-slugに該当する作成済み記事がない場合、NotFoundを返す`() {
            // given:
            val repository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val searchingSlug = Slug.newWithoutValidation("not-found-slug")
            val currentUserId = UserId(1)

            // when:
            val actual = repository.findBySlugFromRegisteredUserViewpoint(searchingSlug, currentUserId)

            // then:
            val expected = ArticleRepository.FindBySlugFromRegisteredUserViewpointError.NotFound(searchingSlug).left()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `異常系-登録済みユーザーが存在しない場合、IllegalArgumentExceptionがthrowされる`() {
            // given: 存在しない登録済みユーザーのid
            val repository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val searchingSlug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin")
            val notExistedUserId = UserId(Int.MAX_VALUE)

            // when:
            val executeFunction = { repository.findBySlugFromRegisteredUserViewpoint(searchingSlug, notExistedUserId) }

            // then:
            Assertions.assertThrows(IllegalArgumentException::class.java) { executeFunction() }
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
