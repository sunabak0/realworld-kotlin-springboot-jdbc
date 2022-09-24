package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import arrow.core.orNull
import arrow.core.right
import arrow.core.toOption
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.UncreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.UpdatableCreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.article.Body
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.infra.helper.SeedData
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.text.SimpleDateFormat
import java.util.Date
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
        fun `正常系-他ユーザーのお気に入りの作成済み記事が1つも無い場、空の作成済み記事の一覧が戻り値`() {
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
            val expected = emptySet<CreatedArticle>().right()
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
            val expected = setOf(
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
        fun `正常系-あるユーザー視点-他ユーザーのお気に入りの作成済み記事が1つも無い場合、空の作成済み記事の一覧が戻り値`() {
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
            val expected = emptySet<CreatedArticle>().right()
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
            val expected = setOf(
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
             * - 他ユーザーが存在しないので、空のセットとなる
             */
            val expected = emptySet<CreatedArticle>().right()
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

    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class Create {
        @BeforeEach
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        @ExpectedDataSet(
            value = ["datasets/yml/then/article_repository/create-success-case-of-simple.yml"],
            ignoreCols = ["id", "slug", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        // NOTE: @ExportDataSetはgivenの@DataSetを変更した時用に残しておく
        // @ExportDataSet(
        //     format = DataSetFormat.YML,
        //     outputName = "src/test/resources/datasets/yml/then/article_repository/create-success-case-of-simple.yml",
        //     includeTables = ["articles", "article_tags", "tags"]
        // )
        fun `正常系-articlesは1つ分増え、tagsとarticle_tagsはタグリスト分だけ増える`() {
            /**
             * given:
             * - 未作成の記事
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val uncreatedArticle = UncreatedArticle.new(
                title = "プログラマーが知るべき97のこと",
                description = "2022年9月時点では、107個ある",
                body = "1. 分別ある行動, 2. 関数型プログラミングを学ぶことの重要性, ...", // TODO: 改行が入っても大丈夫なようにする
                tagList = listOf("エッセイ", "プログラミング"),
                authorId = UserId(1)
            ).orNull()!!

            /**
             * when:
             */
            val actual = articleRepository.create(uncreatedArticle)

            /**
             * then:
             * - 作成済み記事
             */
            when (actual) {
                is Left -> assert(false) { "原因: ${actual.value}" }
                is Right -> {
                    val article = actual.value
                    assertThat(article.slug).isEqualTo(uncreatedArticle.slug)
                    assertThat(article.title).isEqualTo(uncreatedArticle.title)
                    assertThat(article.description).isEqualTo(uncreatedArticle.description)
                    assertThat(article.body).isEqualTo(uncreatedArticle.body)
                    assertThat(article.tagList).hasSameElementsAs(uncreatedArticle.tagList)
                    assertThat(article.authorId).isEqualTo(uncreatedArticle.authorId)
                    assertThat(article.favorited).isFalse
                    assertThat(article.favoritesCount).isEqualTo(0)
                }
            }
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        @ExpectedDataSet(
            value = ["datasets/yml/then/article_repository/create-success-case-of-empty-tag-list.yml"],
            ignoreCols = ["id", "slug", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        // NOTE: @ExportDataSetはgivenの@DataSetを変更した時用に残しておく
        // @ExportDataSet(
        //     format = DataSetFormat.YML,
        //     outputName = "src/test/resources/datasets/yml/then/article_repository/create-success-case-of-empty-tag-list.yml",
        //     includeTables = ["articles", "article_tags", "tags"]
        // )
        fun `正常系-タグリストが空の場合、articlesが1つ分増えるが、tagsとarticle_tagsは増えない`() {
            /**
             * given:
             * - 未作成の記事
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val uncreatedArticle = UncreatedArticle.new(
                title = "ソフトウェアアーキテクトが知るべき97のこと",
                description = "2022年9月時点では、108個ある",
                body = "1. システムの要件よりも履歴書の見栄えを優先させてはならない, 2. 本質的な複雑さは単純に、付随的な複雑さは取り除け, ...",
                tagList = emptyList(),
                authorId = UserId(1)
            ).orNull()!!

            /**
             * when:
             */
            val actual = articleRepository.create(uncreatedArticle)

            /**
             * then:
             * - 作成済み記事
             */
            when (actual) {
                is Left -> assert(false) { "原因: ${actual.value}" }
                is Right -> {
                    val article = actual.value
                    assertThat(article.slug).isEqualTo(uncreatedArticle.slug)
                    assertThat(article.title).isEqualTo(uncreatedArticle.title)
                    assertThat(article.description).isEqualTo(uncreatedArticle.description)
                    assertThat(article.body).isEqualTo(uncreatedArticle.body)
                    assertThat(article.tagList).hasSameElementsAs(uncreatedArticle.tagList)
                    assertThat(article.authorId).isEqualTo(uncreatedArticle.authorId)
                    assertThat(article.favorited).isFalse
                    assertThat(article.favoritesCount).isEqualTo(0)
                }
            }
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        @ExpectedDataSet(
            value = ["datasets/yml/then/article_repository/create-success-case-of-partially-duplicated-tag-list.yml"],
            ignoreCols = ["id", "slug", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        // NOTE: @ExportDataSetはgivenの@DataSetを変更した時用に残しておく
        // @ExportDataSet(
        //     format = DataSetFormat.YML,
        //     outputName = "src/test/resources/datasets/yml/then/article_repository/create-success-case-of-partially-duplicated-tag-list.yml",
        //     includeTables = ["articles", "article_tags", "tags"]
        // )
        fun `正常系-一部のタグが既に保存されている場合、tagsは差分だけ増え、article_tagsはタグリスト分だけ増える`() {
            /**
             * given:
             * - 未作成の記事
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val uncreatedArticle = UncreatedArticle.new(
                title = "Comparing JVM lang",
                description = "JVM",
                body = "",
                tagList = listOf("kotlin", "clojure", "scala", "groovy"),
                authorId = UserId(1)
            ).orNull()!!

            /**
             * when:
             */
            val actual = articleRepository.create(uncreatedArticle)

            /**
             * then:
             * - 作成済み記事
             */
            when (actual) {
                is Left -> assert(false) { "原因: ${actual.value}" }
                is Right -> {
                    val article = actual.value
                    assertThat(article.slug).isEqualTo(uncreatedArticle.slug)
                    assertThat(article.title).isEqualTo(uncreatedArticle.title)
                    assertThat(article.description).isEqualTo(uncreatedArticle.description)
                    assertThat(article.body).isEqualTo(uncreatedArticle.body)
                    assertThat(article.tagList).hasSameElementsAs(uncreatedArticle.tagList)
                    assertThat(article.authorId).isEqualTo(uncreatedArticle.authorId)
                    assertThat(article.favorited).isFalse
                    assertThat(article.favoritesCount).isEqualTo(0)
                }
            }
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        @ExpectedDataSet(
            value = ["datasets/yml/then/article_repository/create-success-case-of-full-duplicated-tag-list.yml"],
            ignoreCols = ["id", "slug", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        // NOTE: @ExportDataSetはgivenの@DataSetを変更した時用に残しておく
        // @ExportDataSet(
        //     format = DataSetFormat.YML,
        //     outputName = "src/test/resources/datasets/yml/then/article_repository/create-success-case-of-full-duplicated-tag-list.yml",
        //     includeTables = ["articles", "article_tags", "tags"]
        // )
        fun `正常系-全てののタグが既に保存されている場合、tagsは増えず、article_tagsはタグリスト分だけ増える`() {
            /**
             * given:
             * - 未作成の記事
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val uncreatedArticle = UncreatedArticle.new(
                title = "Rust vs Scala vs Kotlin2",
                description = "",
                body = "",
                tagList = listOf("kotlin", "scala", "rust"),
                authorId = UserId(1)
            ).orNull()!!

            /**
             * when:
             */
            val actual = articleRepository.create(uncreatedArticle)

            /**
             * then:
             * - 作成済み記事
             */
            when (actual) {
                is Left -> assert(false) { "原因: ${actual.value}" }
                is Right -> {
                    val article = actual.value
                    assertThat(article.slug).isEqualTo(uncreatedArticle.slug)
                    assertThat(article.title).isEqualTo(uncreatedArticle.title)
                    assertThat(article.description).isEqualTo(uncreatedArticle.description)
                    assertThat(article.body).isEqualTo(uncreatedArticle.body)
                    assertThat(article.tagList).hasSameElementsAs(uncreatedArticle.tagList)
                    assertThat(article.authorId).isEqualTo(uncreatedArticle.authorId)
                    assertThat(article.favorited).isFalse
                    assertThat(article.favoritesCount).isEqualTo(0)
                }
            }
        }
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("記事の削除")
    class Delete {
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
            value = ["datasets/yml/then/article_repository/delete-success.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        // NOTE: @ExportDataSetはgivenの@DataSetが変更された時用に残しておく
        // @ExportDataSet(
        //    format = DataSetFormat.YML,
        //    outputName = "src/test/resources/datasets/yml/then/article_repository/delete-success.yml",
        //    includeTables = ["articles", "tags", "article_tags", "favorites", "article_comments"]
        // )
        fun `正常系-存在する作成済み記事を削除した場合、紐づくタグ・お気に入りも削除されるが、タグそのものは削除されない`() {
            /**
             * given:
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val existedArticleId = ArticleId(1)

            /**
             * when:
             */
            val actual = articleRepository.delete(existedArticleId)

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
            value = [
                "datasets/yml/given/articles.yml",
                "datasets/yml/given/tags.yml",
            ],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `準正常系-存在しない作成済み記事を削除しようとした場合、見つからなかった旨のエラーが戻り値`() {
            /**
             * given:
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val notExistedArticleId = ArticleId(99999)

            /**
             * when:
             */
            val actual = articleRepository.delete(notExistedArticleId)

            /**
             * then:
             */
            val expected = ArticleRepository.DeleteError.NotFoundArticle(
                articleId = notExistedArticleId
            ).left()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("記事の更新")
    class Update {
        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/articles.yml",
            ],
        )
        @ExpectedDataSet(
            value = ["datasets/yml/then/article_repository/update-success.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        // NOTE: @ExportDataSetはgivenの@DataSetが変更された時用に残しておく
        // @ExportDataSet(
        //    format = DataSetFormat.YML,
        //    outputName = "src/test/resources/datasets/yml/then/article_repository/update-success.yml",
        //    includeTables = ["articles"]
        // )
        fun `正常系-articlesのidと対応する行が更新される`() {
            /**
             * given:
             * - 存在する作成済み記事
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val updatableCreatedArticle = object : UpdatableCreatedArticle {
                override val articleId: ArticleId get() = ArticleId(1)
                override val title: Title get() = Title.newWithoutValidation("プログラマーが知るべき97のこと")
                override val description: Description get() = Description.newWithoutValidation("エッセイ集")
                override val body: Body get() = Body.newWithoutValidation("93. エラーを無視するな")
                override val updatedAt: Date get() = Date()
            }

            /**
             * when:
             */
            val actual = articleRepository.update(updatableCreatedArticle)

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
            ],
        )
        @ExpectedDataSet(
            value = [
                "datasets/yml/given/articles.yml",
            ],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `準正常系-更新しようとした記事が見つからなかった場合、その旨のエラーが戻り値`() {
            /**
             * given:
             * - 存在しない作成済み記事
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val notExistedUpdatableCreatedArticle = object : UpdatableCreatedArticle {
                override val articleId: ArticleId get() = ArticleId(99999)
                override val title: Title get() = Title.newWithoutValidation("プログラマーが知るべき97のこと")
                override val description: Description get() = Description.newWithoutValidation("エッセイ集")
                override val body: Body get() = Body.newWithoutValidation("52. 「その場しのぎ」が長生きしてしまう")
                override val updatedAt: Date get() = Date()
            }

            /**
             * when:
             */
            val actual = articleRepository.update(notExistedUpdatableCreatedArticle)

            /**
             * then:
             */
            val expected = ArticleRepository.UpdateError.NotFoundArticle(
                articleId = notExistedUpdatableCreatedArticle.articleId
            ).left()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("著者の最新記事郡")
    class LatestByAuthors {
        @BeforeEach
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ],
        )
        fun `正常系-対象の登録済みユーザー視点から見た、指定した著者の最新の作成済み記事郡が戻り値`() {
            /**
             * given:
             * - 指定した著者分(2人分)
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val authors = setOf(
                UserId(1),
                UserId(2),
            )
            val viewpointUserId = UserId(1)

            /**
             * when:
             */
            val actual = articleRepository.latestByAuthors(authors, viewpointUserId)

            /**
             * then:
             * - 詰め替えができているか
             */
            val updatedAtAndIdComparator = Comparator<CreatedArticle> { a, b -> b.updatedAt.compareTo(a.updatedAt) }
                .thenComparator { a, b -> a.id.value.compareTo(b.id.value) }
            val expectedCreatedArticleList = SeedData.createdArticlesFromViewpointSet()[viewpointUserId]!!
                .groupBy { it.authorId } // 著者毎
                .filter { (userId, _) -> authors.contains(userId) } // 指定した著者分に含まれているかどうか
                .values // 著者の作成済み記事のList
                .map { it.sortedWith(updatedAtAndIdComparator).first() } // それぞれ更新日時が最新(もし被っていたらIdの若い方)

            when (actual) {
                is Left -> assert(false) { "原因: ${actual.value}" }
                is Right -> {
                    val actualArticles = actual.value
                    actualArticles.forEach { actualArticle ->
                        val expected = expectedCreatedArticleList.find { it.id == actualArticle.id }!!

                        val softly = SoftAssertions()
                        softly.assertThat(actualArticle.title).isEqualTo(expected.title)
                        softly.assertThat(actualArticle.description).isEqualTo(expected.description)
                        softly.assertThat(actualArticle.body).isEqualTo(expected.body)
                        softly.assertThat(actualArticle.slug).isEqualTo(expected.slug)
                        softly.assertThat(actualArticle.tagList).isEqualTo(expected.tagList)
                        softly.assertThat(actualArticle.createdAt).isEqualTo(expected.createdAt)
                        softly.assertThat(actualArticle.updatedAt).isEqualTo(expected.updatedAt)
                        softly.assertThat(actualArticle.favorited).isEqualTo(expected.favorited)
                        softly.assertThat(actualArticle.favoritesCount).isEqualTo(expected.favoritesCount)
                        softly.assertAll()
                    }
                    assertThat(actualArticles.size)
                        .`as`("取得できる数が一致する")
                        .isEqualTo(expectedCreatedArticleList.size)
                }
            }
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ],
        )
        fun `正常系-著者を誰も指定しなかった場合、空の作成済み記事郡が戻り値`() {
            /**
             * given:
             * - 著者を指定しない
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val authors = emptySet<UserId>()
            val viewpointUserId = UserId(1)

            /**
             * when:
             */
            val actual = articleRepository.latestByAuthors(authors, viewpointUserId)

            /**
             * then:
             */
            val expected = emptySet<CreatedArticle>().right()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ],
        )
        fun `正常系-指定した著者が記事を書いていなかった場合、その著者分の最新記事は無い`() {
            /**
             * given:
             * - 記事を書いていない著者(1人分)
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val authors = setOf(
                UserId(3),
            )
            val viewpointUserId = UserId(1)

            /**
             * when:
             */
            val actual = articleRepository.latestByAuthors(authors, viewpointUserId)

            /**
             * then:
             */
            val expected = emptySet<CreatedArticle>().right()
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ],
        )
        fun `正常系-存在しない著者を指定した場合、その著者分の最新記事は無い`() {
            /**
             * given:
             * - 存在しない著者(3人分)
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val authors = setOf(
                UserId(SeedData.users().size + 1),
                UserId(SeedData.users().size + 2),
                UserId(SeedData.users().size + 3),
            )
            val viewpointUserId = UserId(1)

            /**
             * when:
             */
            val actual = articleRepository.latestByAuthors(authors, viewpointUserId)

            /**
             * then:
             */
            val expected = emptySet<CreatedArticle>().right()
            assertThat(actual).isEqualTo(expected)
        }

        /**
         * 基本ありえない/あってはいけない
         * 引数的にはできてしまう、例外も投げられないので、注意を込めたテスト
         */
        @Test
        @DataSet(
            value = [
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ],
        )
        fun `仕様外-視点となる登録済みユーザーに存在しない著者を指定した場合、全ての作成済み記事のお気に入り状態は全てfalse`() {
            /**
             * given:
             * - 著者は登録済みユーザー全員分
             */
            val articleRepository = ArticleRepositoryImpl(DbConnection.namedParameterJdbcTemplate)
            val authors = SeedData.users().groupBy { it.userId }.keys
            val viewpointUserId = UserId(SeedData.users().size + 1)

            /**
             * when:
             */
            val actual = articleRepository.latestByAuthors(authors, viewpointUserId)

            /**
             * then:
             * - 取得できる作成済み記事の数と各作成済み記事Idは一致する
             * - お気に入り状態は全て非お気に入り
             */
            val updatedAtAndIdComparator = Comparator<CreatedArticle> { a, b -> b.updatedAt.compareTo(a.updatedAt) }
                .thenComparator { a, b -> a.id.value.compareTo(b.id.value) }
            val expected = SeedData.createdArticles()
                .groupBy { it.authorId } // 著者毎にグルーピング
                .filter { (userId, _) -> authors.contains(userId) } // 指定した著者だけ
                .values // 著者毎の作成済み記事のリスト
                .map { it.sortedWith(updatedAtAndIdComparator).first() } // それぞれ更新日時が最新(もし被っていたらIdの若い方)
                .toSet()

            when (actual) {
                is Left -> assert(false) { "原因: ${actual.value}" }
                is Right -> {
                    val articles = actual.value
                    assertThat(articles)
                        .`as`("取得できる作成済み記事の数と各作成済み記事Idは一致する")
                        .isEqualTo(expected)

                    articles.forEach {
                        assertThat(it.favorited)
                            .`as`("お気に入り状態は非お気に入り")
                            .isEqualTo(false)
                    }
                }
            }
        }
    }
}
