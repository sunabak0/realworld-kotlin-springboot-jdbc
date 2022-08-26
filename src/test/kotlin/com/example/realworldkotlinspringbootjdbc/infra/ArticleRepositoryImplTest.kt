package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.article.Body
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.text.SimpleDateFormat
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag as ArticleTag

class ArticleRepositoryImplTest {
    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class All {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/empty-articles.yml"
            ]
        )
        fun `正常系-作成済み記事が1つも無い場合、空の作成済み記事の一覧が戻り値`() {
            /**
             * given:
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)

            /**
             * when:
             */
            val actual = articleRepository.all()

            /**
             * then:
             */
            val expected = listOf<CreatedArticle>().right()
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
        fun `正常系-作成済み記事がN個だけある場合、長さがNの作成済み記事の一覧が戻り値`() {
            /**
             * given:
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)

            /**
             * when:
             */
            val actual = articleRepository.all()

            /**
             * then:
             * - created_at, updated_at以外の中身を比較する(想定したカラムの中身がきちんとセットされているか)
             */
            val expected = listOf(
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(1),
                    title = Title.newWithoutValidation("Rust vs Scala vs Kotlin"),
                    slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                    body = Body.newWithoutValidation("dummy-body"),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    description = Description.newWithoutValidation("dummy-description"),
                    tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                    authorId = UserId(1),
                    favorited = false,
                    favoritesCount = 1
                ),
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(2),
                    title = Title.newWithoutValidation("Functional programming kotlin"),
                    slug = Slug.newWithoutValidation("functional-programming-kotlin"),
                    body = Body.newWithoutValidation("dummy-body"),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    description = Description.newWithoutValidation("dummy-description"),
                    tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                    authorId = UserId(1),
                    favorited = false,
                    favoritesCount = 1
                ),
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(3),
                    title = Title.newWithoutValidation("TDD(Type Driven Development)"),
                    slug = Slug.newWithoutValidation("tdd-type-driven-development"),
                    body = Body.newWithoutValidation("dummy-body"),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    description = Description.newWithoutValidation("dummy-description"),
                    tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                    authorId = UserId(2),
                    favorited = false,
                    favoritesCount = 2
                ),
            )
            when (actual) {
                is Left -> assert(false) { "原因: ${actual.value}" }
                is Right -> {
                    val createdArticleList = actual.value
                    assertThat(createdArticleList).hasSameElementsAs(expected) // サイズとCreatedArticle#equalsで確認
                    createdArticleList.forEach { actualArticle ->
                        val expectedArticle = expected.find { it.id == actualArticle.id }!! // 上のhasSameElementsAsで必ず存在することが確定している
                        assertThat(actualArticle.id).isEqualTo(expectedArticle.id)
                        assertThat(actualArticle.title).isEqualTo(expectedArticle.title)
                        assertThat(actualArticle.slug).isEqualTo(expectedArticle.slug)
                        assertThat(actualArticle.body).isEqualTo(expectedArticle.body)
                        assertThat(actualArticle.description).isEqualTo(expectedArticle.description)
                        assertThat(actualArticle.authorId).isEqualTo(expectedArticle.authorId)
                        assertThat(actualArticle.favorited).isEqualTo(expectedArticle.favorited)
                        assertThat(actualArticle.favoritesCount).isEqualTo(expectedArticle.favoritesCount)
                    }
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
        fun `正常系-あるユーザー視点-作成済み記事が1つも無い場合、空の作成済み記事の一覧が戻り値`() {
            /**
             * given:
             * - 視点となるユーザーId
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val viewpointUserId = UserId(1).toOption()

            /**
             * when:
             */
            val actual = articleRepository.all(viewpointUserId)

            /**
             * then:
             */
            val expected = listOf<CreatedArticle>().right()
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
        fun `正常系-あるユーザー視点-作成済み記事がN個だけある場合、長さがNのお気に入りの有無が有る作成済み記事の一覧が戻り値`() {
            /**
             * given:
             * - 視点となるユーザーId
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val viewpointUserId = UserId(2).toOption()

            /**
             * when:
             */
            val actual = articleRepository.all(viewpointUserId)

            /**
             * then:
             * - created_at, updated_at以外の中身を比較する(想定したカラムの中身がきちんとセットされているか)
             */
            val expected = listOf(
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(1),
                    title = Title.newWithoutValidation("Rust vs Scala vs Kotlin"),
                    slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                    body = Body.newWithoutValidation("dummy-body"),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    description = Description.newWithoutValidation("dummy-description"),
                    tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                    authorId = UserId(1),
                    favorited = true,
                    favoritesCount = 1
                ),
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(2),
                    title = Title.newWithoutValidation("Functional programming kotlin"),
                    slug = Slug.newWithoutValidation("functional-programming-kotlin"),
                    body = Body.newWithoutValidation("dummy-body"),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    description = Description.newWithoutValidation("dummy-description"),
                    tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                    authorId = UserId(1),
                    favorited = false,
                    favoritesCount = 1
                ),
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(3),
                    title = Title.newWithoutValidation("TDD(Type Driven Development)"),
                    slug = Slug.newWithoutValidation("tdd-type-driven-development"),
                    body = Body.newWithoutValidation("dummy-body"),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    description = Description.newWithoutValidation("dummy-description"),
                    tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                    authorId = UserId(2),
                    favorited = true,
                    favoritesCount = 2
                ),
            )
            when (actual) {
                is Left -> assert(false) { "原因: ${actual.value}" }
                is Right -> {
                    val createdArticleList = actual.value
                    assertThat(createdArticleList).hasSameElementsAs(expected) // サイズとCreatedArticle#equalsで確認
                    createdArticleList.forEach { actualArticle ->
                        val expectedArticle = expected.find { it.id == actualArticle.id }!! // 上のhasSameElementsAsで必ず存在することが確定している
                        assertThat(actualArticle.id).isEqualTo(expectedArticle.id)
                        assertThat(actualArticle.title).isEqualTo(expectedArticle.title)
                        assertThat(actualArticle.slug).isEqualTo(expectedArticle.slug)
                        assertThat(actualArticle.body).isEqualTo(expectedArticle.body)
                        assertThat(actualArticle.description).isEqualTo(expectedArticle.description)
                        assertThat(actualArticle.authorId).isEqualTo(expectedArticle.authorId)
                        assertThat(actualArticle.favorited).isEqualTo(expectedArticle.favorited)
                        assertThat(actualArticle.favoritesCount).isEqualTo(expectedArticle.favoritesCount)
                    }
                }
            }
        }

        /**
         * ユースケース上はありえない
         * 技術詳細(引数)的には可能なためテストを記述
         */
        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `仕様外-存在しないユーザー視点-作成済み記事一覧しても例外は起きない`() {
            /**
             * given:
             * - 存在しない視点となるユーザーId
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val notExistedviewpointUserId = UserId(-1).toOption()

            /**
             * when:
             */
            val actual = articleRepository.all(notExistedviewpointUserId)

            /**
             * then:
             * - created_at, updated_at以外の中身を比較する(想定したカラムの中身がきちんとセットされているか)
             * - favoritedが全てfalse
             */
            val expected = listOf(
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(1),
                    title = Title.newWithoutValidation("Rust vs Scala vs Kotlin"),
                    slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                    body = Body.newWithoutValidation("dummy-body"),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    description = Description.newWithoutValidation("dummy-description"),
                    tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                    authorId = UserId(1),
                    favorited = false,
                    favoritesCount = 1
                ),
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(2),
                    title = Title.newWithoutValidation("Functional programming kotlin"),
                    slug = Slug.newWithoutValidation("functional-programming-kotlin"),
                    body = Body.newWithoutValidation("dummy-body"),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    description = Description.newWithoutValidation("dummy-description"),
                    tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                    authorId = UserId(1),
                    favorited = false,
                    favoritesCount = 1
                ),
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(3),
                    title = Title.newWithoutValidation("TDD(Type Driven Development)"),
                    slug = Slug.newWithoutValidation("tdd-type-driven-development"),
                    body = Body.newWithoutValidation("dummy-body"),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    description = Description.newWithoutValidation("dummy-description"),
                    tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                    authorId = UserId(2),
                    favorited = false,
                    favoritesCount = 2
                ),
            )
            when (actual) {
                is Left -> assert(false) { "原因: ${actual.value}" }
                is Right -> {
                    val createdArticleList = actual.value
                    assertThat(createdArticleList).hasSameElementsAs(expected) // サイズとCreatedArticle#equalsで確認
                    createdArticleList.forEach { actualArticle ->
                        val expectedArticle = expected.find { it.id == actualArticle.id }!! // 上のhasSameElementsAsで必ず存在することが確定している
                        assertThat(actualArticle.id).isEqualTo(expectedArticle.id)
                        assertThat(actualArticle.title).isEqualTo(expectedArticle.title)
                        assertThat(actualArticle.slug).isEqualTo(expectedArticle.slug)
                        assertThat(actualArticle.body).isEqualTo(expectedArticle.body)
                        assertThat(actualArticle.description).isEqualTo(expectedArticle.description)
                        assertThat(actualArticle.authorId).isEqualTo(expectedArticle.authorId)
                        assertThat(actualArticle.favorited).isEqualTo(expectedArticle.favorited)
                        assertThat(actualArticle.favoritesCount).isEqualTo(expectedArticle.favoritesCount)
                    }
                }
            }
        }
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class FilterFavoritedByOtherUserId {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/empty-articles.yml"
            ]
        )
        fun `正常系-他ユーザーのお気に入りの作成済み記事が1つも無い場合作成済み記事が1つも無い場合、空の作成済み記事の一覧が戻り値`() {
            /**
             * given:
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val otherUserId = UserId(2)

            /**
             * when:
             */
            val actual = articleRepository.filterFavoritedByOtherUserId(otherUserId)

            /**
             * then:
             */
            val expected = emptyList<CreatedArticle>().right()
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
        fun `正常系-他ユーザーのお気に入りの作成済み記事がN個だけある場合、長さがNの作成済み記事の一覧が戻り値`() {
            /**
             * given:
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val otherUserId = UserId(2)

            /**
             * when:
             */
            val actual = articleRepository.filterFavoritedByOtherUserId(otherUserId)

            /**
             * then:
             * - created_at, updated_at以外の中身を比較する(想定したカラムの中身がきちんとセットされているか)
             * - favoritedは全てfalse
             */
            val expected = listOf(
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(1),
                    title = Title.newWithoutValidation("Rust vs Scala vs Kotlin"),
                    slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                    body = Body.newWithoutValidation("dummy-body"),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    description = Description.newWithoutValidation("dummy-description"),
                    tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                    authorId = UserId(1),
                    favorited = false,
                    favoritesCount = 1
                ),
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(3),
                    title = Title.newWithoutValidation("TDD(Type Driven Development)"),
                    slug = Slug.newWithoutValidation("tdd-type-driven-development"),
                    body = Body.newWithoutValidation("dummy-body"),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    description = Description.newWithoutValidation("dummy-description"),
                    tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                    authorId = UserId(2),
                    favorited = false,
                    favoritesCount = 2
                ),
            )
            when (actual) {
                is Left -> assert(false) { "原因: ${actual.value}" }
                is Right -> {
                    val createdArticleList = actual.value
                    assertThat(createdArticleList).hasSameElementsAs(expected) // サイズとCreatedArticle#equalsで確認
                    createdArticleList.forEach { actualArticle ->
                        val expectedArticle = expected.find { it.id == actualArticle.id }!! // 上のhasSameElementsAsで必ず存在することが確定している
                        assertThat(actualArticle.id).isEqualTo(expectedArticle.id)
                        assertThat(actualArticle.title).isEqualTo(expectedArticle.title)
                        assertThat(actualArticle.slug).isEqualTo(expectedArticle.slug)
                        assertThat(actualArticle.body).isEqualTo(expectedArticle.body)
                        assertThat(actualArticle.description).isEqualTo(expectedArticle.description)
                        assertThat(actualArticle.authorId).isEqualTo(expectedArticle.authorId)
                        assertThat(actualArticle.favorited).isEqualTo(expectedArticle.favorited)
                        assertThat(actualArticle.favoritesCount).isEqualTo(expectedArticle.favoritesCount)
                    }
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
        fun `正常系-あるユーザー視点-他ユーザーのお気に入りの作成済み記事が1つも無い場合作成済み記事が1つも無い場合、空の作成済み記事の一覧が戻り値`() {
            /**
             * given:
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val otherUserId = UserId(2)
            val viewpointUserId = UserId(1).toOption()

            /**
             * when:
             */
            val actual = articleRepository.filterFavoritedByOtherUserId(otherUserId, viewpointUserId)

            /**
             * then:
             */
            val expected = emptyList<CreatedArticle>().right()
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
        fun `正常系-あるユーザー視点-他ユーザーのお気に入りの作成済み記事がN個だけある場合、長さがNの作成済み記事の一覧が戻り値`() {
            /**
             * given:
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val otherUserId = UserId(2)
            val viewpointUserId = UserId(1).toOption()

            /**
             * when:
             */
            val actual = articleRepository.filterFavoritedByOtherUserId(otherUserId, viewpointUserId)

            /**
             * then:
             * - created_at, updated_at以外の中身を比較する(想定したカラムの中身がきちんとセットされているか)
             * - あるユーザー視点からの お気に入り or 非お気に入り 情報がある
             */
            val expected = listOf(
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(1),
                    title = Title.newWithoutValidation("Rust vs Scala vs Kotlin"),
                    slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                    body = Body.newWithoutValidation("dummy-body"),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    description = Description.newWithoutValidation("dummy-description"),
                    tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                    authorId = UserId(1),
                    favorited = false,
                    favoritesCount = 1
                ),
                CreatedArticle.newWithoutValidation(
                    id = ArticleId(3),
                    title = Title.newWithoutValidation("TDD(Type Driven Development)"),
                    slug = Slug.newWithoutValidation("tdd-type-driven-development"),
                    body = Body.newWithoutValidation("dummy-body"),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    description = Description.newWithoutValidation("dummy-description"),
                    tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                    authorId = UserId(2),
                    favorited = true,
                    favoritesCount = 2
                ),
            )
            when (actual) {
                is Left -> assert(false) { "原因: ${actual.value}" }
                is Right -> {
                    val createdArticleList = actual.value
                    assertThat(createdArticleList).hasSameElementsAs(expected) // サイズとCreatedArticle#equalsで確認
                    createdArticleList.forEach { actualArticle ->
                        val expectedArticle = expected.find { it.id == actualArticle.id }!! // 上のhasSameElementsAsで必ず存在することが確定している
                        assertThat(actualArticle.id).isEqualTo(expectedArticle.id)
                        assertThat(actualArticle.title).isEqualTo(expectedArticle.title)
                        assertThat(actualArticle.slug).isEqualTo(expectedArticle.slug)
                        assertThat(actualArticle.body).isEqualTo(expectedArticle.body)
                        assertThat(actualArticle.description).isEqualTo(expectedArticle.description)
                        assertThat(actualArticle.authorId).isEqualTo(expectedArticle.authorId)
                        assertThat(actualArticle.favorited).isEqualTo(expectedArticle.favorited)
                        assertThat(actualArticle.favoritesCount).isEqualTo(expectedArticle.favoritesCount)
                    }
                }
            }
        }

        /**
         * ユースケース上はありえない
         * 技術詳細(引数)的には可能なためテストを記述
         */
        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `仕様外-存在しない他ユーザーのお気に入りの作成済み記事をフィルタしても例外は起きない`() {
            /**
             * given:
             * - 存在しない他ユーザーId
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val otherUserId = UserId(-1)

            /**
             * when:
             */
            val actual = articleRepository.filterFavoritedByOtherUserId(otherUserId)

            /**
             * then:
             * - 他ユーザーが存在しないので、空のリストとなる
             */
            val expected = emptyList<CreatedArticle>().right()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("Slugで記事検索")
    class FindBySlugTest {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

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
        fun reset() = DbConnection.resetSequence()

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
        fun `準正常系-slugに該当する作成済み記事がない場合、NotFoundArticleを返す`() {
            // given:
            val repository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val searchingSlug = Slug.newWithoutValidation("not-found-slug")
            val currentUserId = UserId(1)

            // when:
            val actual = repository.findBySlugFromRegisteredUserViewpoint(searchingSlug, currentUserId)

            // then:
            val expected =
                ArticleRepository.FindBySlugFromRegisteredUserViewpointError.NotFoundArticle(searchingSlug).left()
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
        fun `準正常系-登録済みユーザーが存在しない場合、NotFoundUserを返す`() {
            // given: 存在しない登録済みユーザーのid
            val repository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val searchingSlug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin")
            val notExistedUserId = UserId(Int.MAX_VALUE)

            // when:
            val actual = repository.findBySlugFromRegisteredUserViewpoint(searchingSlug, notExistedUserId)

            // then:
            val expected =
                ArticleRepository.FindBySlugFromRegisteredUserViewpointError.NotFoundUser(notExistedUserId).left()
            assertThat(actual).isEqualTo(expected)
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

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("お気に入り登録")
    class Favorite {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/articles.yml",
                "datasets/yml/given/tags.yml",
            ],
        )
        @ExpectedDataSet(
            value = ["datasets/yml/then/article_repository/favorite-success.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `正常系-「slug に該当する articles テーブルに作成済記事が存在する」「favorites テーブルにお気に入り登録済でない」場合は、お気に入り登録（favorites テーブルに挿入）され 作成済記事（CreatedArticle）が戻り値`() {
            /**
             * given:
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)

            /**
             * when:
             * - slug に該当する作成済記事が存在する（articles テーブルに該当データが存在する）
             * - slug に該当する記事がお気に入り登録済でない（favorites テーブルに該当データが存在しない）
             */
            val actual = articleRepository.favorite(
                slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                currentUserId = UserId(3)
            )

            /**
             * then:
             */
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val expected = CreatedArticle.newWithoutValidation(
                id = ArticleId(1),
                title = Title.newWithoutValidation("Rust vs Scala vs Kotlin"),
                slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                body = Body.newWithoutValidation("dummy-body"),
                createdAt = date, // 比較しない
                updatedAt = date, // 比較しない
                description = Description.newWithoutValidation("dummy-description"),
                tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                authorId = UserId(1),
                favorited = true,
                favoritesCount = 2
            )
            when (actual) {
                is Left -> assert(false)
                is Right -> {
                    // createdAt と updatedAt はメタデータなので比較しない
                    assertThat(actual.value.id).isEqualTo(expected.id)
                    assertThat(actual.value.title).isEqualTo(expected.title)
                    assertThat(actual.value.slug).isEqualTo(expected.slug)
                    assertThat(actual.value.body).isEqualTo(expected.body)
                    assertThat(actual.value.description).isEqualTo(expected.description)
                    assertThat(actual.value.authorId).isEqualTo(expected.authorId)
                    assertThat(actual.value.favorited).isEqualTo(expected.favorited)
                    assertThat(actual.value.favoritesCount).isEqualTo(expected.favoritesCount)
                }
            }
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/articles.yml",
                "datasets/yml/given/tags.yml",
            ],
        )
        @ExpectedDataSet(
            value = ["datasets/yml/given/articles.yml", "datasets/yml/given/tags.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `正常系-「articles テーブルに slug に該当する作成済記事が存在」「favorites テーブルにお気に入り登録済」の場合は、お気に入り登録されず（テーブルに変更なし）作成済記事（CreatedArticle）が戻り値`() {
            /**
             * given:
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)

            /**
             * when:
             * - slug に該当する作成済記事が存在する（articles テーブルに該当データが存在する）
             * - slug に該当する記事がお気に入り登録済み（favorites テーブルに該当データが存在する）
             */
            val actual = articleRepository.favorite(
                slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"), // slug に該当する記事は既にお気に入り済
                currentUserId = UserId(2)
            )

            /**
             * then:
             * - 追加でお気に入り登録されない（articles テーブルに変更なし）
             * - 戻り値は slug に該当する作成済記事（CreatedArticle）
             */
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val expected = CreatedArticle.newWithoutValidation(
                id = ArticleId(1),
                title = Title.newWithoutValidation("Rust vs Scala vs Kotlin"),
                slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                body = Body.newWithoutValidation("dummy-body"),
                createdAt = date, // 比較しない
                updatedAt = date, // 比較しない
                description = Description.newWithoutValidation("dummy-description"),
                tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                authorId = UserId(1),
                favorited = true,
                favoritesCount = 1
            )
            when (actual) {
                is Left -> assert(false)
                is Right -> {
                    // createdAt と updatedAt はメタデータなので比較しない
                    assertThat(actual.value.id).isEqualTo(expected.id)
                    assertThat(actual.value.title).isEqualTo(expected.title)
                    assertThat(actual.value.slug).isEqualTo(expected.slug)
                    assertThat(actual.value.body).isEqualTo(expected.body)
                    assertThat(actual.value.description).isEqualTo(expected.description)
                    assertThat(actual.value.authorId).isEqualTo(expected.authorId)
                    assertThat(actual.value.favorited).isEqualTo(expected.favorited)
                    assertThat(actual.value.favoritesCount).isEqualTo(expected.favoritesCount)
                }
            }
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/articles.yml",
                "datasets/yml/given/tags.yml",
            ],
        )
        @ExpectedDataSet(
            value = ["datasets/yml/given/articles.yml", "datasets/yml/given/tags.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `準正常系-「articles テーブルに slug に該当する作成済記事が存在しない」場合は、お気に入り登録されず（テーブルに変更なし）、NotFoundCreatedArticleBySlug が戻り値`() {
            /**
             * given:
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)

            /**
             * when:
             * - slug に該当する作成済記事が存在しない
             */
            val actual = articleRepository.favorite(
                slug = Slug.newWithoutValidation("not-existed-dummy-slug"), // slug に該当する作成済記事が存在しない
                currentUserId = UserId(3)
            )

            /**
             * then:
             */
            val expected =
                ArticleRepository.FavoriteError.NotFoundCreatedArticleBySlug(slug = Slug.newWithoutValidation("not-existed-dummy-slug"))
                    .left()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("お気に入り登録解除")
    class Unfavorite {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/articles.yml",
                "datasets/yml/given/tags.yml",
            ],
        )
        @ExpectedDataSet(
            value = ["datasets/yml/then/article_repository/unfavorite-success.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `正常系-「slug に該当する articles テーブルに作成済記事が存在する」「favorites テーブルに「作成済記事に該当する article_id」かつ「user_id が実行ユーザー」」の場合は、お気に入り解除（favorites テーブルから削除）され 作成済記事（CreatedArticle）が戻り値`() {
            /**
             * given:
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)

            /**
             * when:
             * - slug に該当する作成済記事が articles テーブルに存在する
             * - favorites テーブルに作成済記事に該当する article_id かつ user_id が 実行ユーザーと同じ
             */
            val actual = articleRepository.unfavorite(
                slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"), // slug に該当する作成済記事が articles テーブルに存在する
                currentUserId = UserId(2) // favorites テーブルに作成済記事に該当する article_id かつ user_id が 実行ユーザーと同じ
            )

            /**
             * then:
             * - お気に入り解除（favorites テーブルから対象レコードが削除）
             * - 作成済記事（CreatedArticle）が戻り値
             */
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val expected = CreatedArticle.newWithoutValidation(
                id = ArticleId(1),
                title = Title.newWithoutValidation("Rust vs Scala vs Kotlin"),
                slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                body = Body.newWithoutValidation("dummy-body"),
                createdAt = date, // 比較しない
                updatedAt = date, // 比較しない
                description = Description.newWithoutValidation("dummy-description"),
                tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                authorId = UserId(1),
                favorited = false,
                favoritesCount = 0
            )
            when (actual) {
                is Left -> assert(false)
                is Right -> {
                    // createdAt と updatedAt はメタデータなので比較しない
                    assertThat(actual.value.id).isEqualTo(expected.id)
                    assertThat(actual.value.title).isEqualTo(expected.title)
                    assertThat(actual.value.slug).isEqualTo(expected.slug)
                    assertThat(actual.value.body).isEqualTo(expected.body)
                    assertThat(actual.value.description).isEqualTo(expected.description)
                    assertThat(actual.value.authorId).isEqualTo(expected.authorId)
                    assertThat(actual.value.favorited).isEqualTo(expected.favorited)
                    assertThat(actual.value.favoritesCount).isEqualTo(expected.favoritesCount)
                }
            }
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/articles.yml",
                "datasets/yml/given/tags.yml",
            ],
        )
        @ExpectedDataSet(
            value = [
                "datasets/yml/given/articles.yml",
                "datasets/yml/given/tags.yml",
            ],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `正常系-「slug に該当する articles テーブルに作成済記事が存在する」「favorites テーブルに「作成済記事に該当する article_id」かつ「user_id が実行ユーザーでない」」の場合は、お気に入り解除されず（favorites テーブル変更なし）、 作成済記事（CreatedArticle）が戻り値`() {
            /**
             * given:
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)

            /**
             * when:
             * - slug に該当する作成済記事が articles テーブルに存在する
             * - favorites テーブルに作成済記事に該当する article_id かつ user_id が 実行ユーザーと同じでない
             */
            val actual = articleRepository.unfavorite(
                slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                currentUserId = UserId(1)
            )

            /**
             * then:
             * - お気に入り解除されない（favorites テーブルに変更なし）
             * - 作成済記事が戻り値
             */
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val expected = CreatedArticle.newWithoutValidation(
                id = ArticleId(1),
                title = Title.newWithoutValidation("Rust vs Scala vs Kotlin"),
                slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                body = Body.newWithoutValidation("dummy-body"),
                createdAt = date, // 比較しない
                updatedAt = date, // 比較しない
                description = Description.newWithoutValidation("dummy-description"),
                tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                authorId = UserId(1),
                favorited = false,
                favoritesCount = 1
            )
            when (actual) {
                is Left -> assert(false)
                is Right -> {
                    // createdAt と updatedAt はメタデータなので比較しない
                    assertThat(actual.value.id).isEqualTo(expected.id)
                    assertThat(actual.value.title).isEqualTo(expected.title)
                    assertThat(actual.value.slug).isEqualTo(expected.slug)
                    assertThat(actual.value.body).isEqualTo(expected.body)
                    assertThat(actual.value.description).isEqualTo(expected.description)
                    assertThat(actual.value.authorId).isEqualTo(expected.authorId)
                    assertThat(actual.value.favorited).isEqualTo(expected.favorited)
                    assertThat(actual.value.favoritesCount).isEqualTo(expected.favoritesCount)
                }
            }
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/articles.yml",
                "datasets/yml/given/tags.yml",
            ],
        )
        @ExpectedDataSet(
            value = [
                "datasets/yml/given/articles.yml",
                "datasets/yml/given/tags.yml",
            ],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `準正常系-「slug に該当する articles テーブルに作成済記事が存在しない」の場合は、お気に入り解除されず（favorites テーブル変更なし）、 NotFoundCreatedArticleBySlug が戻り値`() {
            /**
             * given:
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)

            /**
             * when:
             * - slug に該当する作成済記事が articles テーブルに存在しない
             */
            val actual = articleRepository.unfavorite(
                slug = Slug.newWithoutValidation("not-existed-dummy-slug"), // slug に該当する作成済記事が存在しない
                currentUserId = UserId(1) // 実行ユーザーの UserId に意味はない
            )

            /**
             * then:
             * - お気に入り解除されない（favorites テーブルに変更なし）
             * - NotFoundCreatedArticleBySlug が戻り値
             */
            val expected =
                ArticleRepository.UnfavoriteError.NotFoundCreatedArticleBySlug(Slug.newWithoutValidation("not-existed-dummy-slug"))
                    .left()
            assertThat(actual).isEqualTo(expected)
        }
    }
}
