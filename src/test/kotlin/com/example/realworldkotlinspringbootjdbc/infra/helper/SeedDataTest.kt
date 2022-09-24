package com.example.realworldkotlinspringbootjdbc.infra.helper

import com.example.realworldkotlinspringbootjdbc.infra.DbConnection
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import java.text.SimpleDateFormat
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag as ArticleTag

/**
 * テストのためのテスト
 *
 * DataSet(Seed)の変更に気づくためのテスト
 */
class SeedDataTest {
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class Users {
        @BeforeEach
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet("datasets/yml/given/users.yml")
        fun `SeedData#users()はDBのSeedDataを網羅している`() {
            /**
             * given:
             */

            /**
             * when:
             */
            val actualUsers = SeedData.users()

            /**
             * then:
             * - DBにあるレコードの中身とそれぞれ一致する
             * - DBにあるレコード数が一致する
             */
            val expectedUsers = DbConnection.namedParameterJdbcTemplate.queryForList(
                """
                    SELECT
                        users.id
                        , users.email
                        , users.username
                        , profiles.bio
                        , profiles.image
                    FROM
                        users
                    JOIN
                        profiles
                    ON
                        profiles.user_id = users.id
                    ;
                """.trimIndent(),
                MapSqlParameterSource()
            ).sortedBy { it["id"].toString().toInt() }

            actualUsers.forEach { actualUser ->
                val expectedUserMap = expectedUsers.find {
                    it["id"].toString().toInt() == actualUser.userId.value
                }!!

                val softly = SoftAssertions()
                softly.assertThat(actualUser.email.value).isEqualTo(expectedUserMap["email"].toString())
                softly.assertThat(actualUser.username.value).isEqualTo(expectedUserMap["username"].toString())
                softly.assertThat(actualUser.bio.value).isEqualTo(expectedUserMap["bio"].toString())
                softly.assertThat(actualUser.image.value).isEqualTo(expectedUserMap["image"].toString())
                softly.assertAll()
            }

            assertThat(actualUsers.size)
                .`as`("レコード総数と一致する")
                .isEqualTo(expectedUsers.size)
        }
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class Tags {
        @BeforeEach
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet("datasets/yml/given/tags.yml")
        fun `SeedData#tags()はDBのSeedDataを網羅している`() {
            /**
             * given:
             */

            /**
             * when:
             */
            val actualTags = SeedData.tags()

            /**
             * then:
             * - DBに存在する
             * - DBのレコード数と一致する
             */
            val expectedTags = DbConnection.namedParameterJdbcTemplate.queryForList(
                """
                    SELECT
                        name
                    FROM
                        tags
                    ;
                """.trimIndent(),
                MapSqlParameterSource()
            )
            actualTags.forEach { actualTag ->
                val foundTag = expectedTags.find {
                    it["name"].toString() == actualTag.value
                }

                assertThat(foundTag)
                    .`as`("$actualTag がDBに存在する")
                    .isNotNull
            }

            assertThat(actualTags.size)
                .`as`("レコード総数と一致する")
                .isEqualTo(expectedTags.size)
        }
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class CreatedArticles {
        @BeforeEach
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `SeedData#createdArticles()はDBのSeedDataを網羅している`() {
            /**
             * given:
             */

            /**
             * when:
             */
            val actualCreatedArticles = SeedData.createdArticles()

            /**
             * then:
             * - DBにあるレコードの中身とそれぞれ一致する
             * - DBのレコード数と一致する
             */
            val expectedCreatedArticles = DbConnection.namedParameterJdbcTemplate.queryForList(
                """
                    SELECT
                        articles.id
                        , articles.title
                        , articles.slug
                        , articles.body
                        , articles.created_at
                        , articles.updated_at
                        , articles.description
                        , COALESCE((
                            SELECT
                                STRING_AGG(tags.name, ',')
                            FROM
                                tags
                            JOIN
                                article_tags
                            ON
                                article_tags.tag_id = tags.id
                                AND article_tags.article_id = articles.id
                            GROUP BY
                                article_tags.article_id
                        ), '') AS tags
                        , articles.author_id
                        , (
                            SELECT
                                COUNT(favorites.id)
                            FROM
                                favorites
                            WHERE
                                favorites.article_id = articles.id
                        ) AS favorites_count
                    FROM
                        articles
                    ;                   
                """.trimIndent(),
                MapSqlParameterSource()
            )
            actualCreatedArticles.forEach { actualCreatedArticle ->
                val expectedCreatedArticle = expectedCreatedArticles.find {
                    it["id"].toString().toInt() == actualCreatedArticle.id.value
                }!!

                val softly = SoftAssertions()
                softly.assertThat(actualCreatedArticle.title.value)
                    .isEqualTo(expectedCreatedArticle["title"].toString())
                softly.assertThat(actualCreatedArticle.body.value).isEqualTo(expectedCreatedArticle["body"].toString())
                softly.assertThat(actualCreatedArticle.description.value)
                    .isEqualTo(expectedCreatedArticle["description"].toString())
                softly.assertThat(actualCreatedArticle.authorId.value)
                    .isEqualTo(expectedCreatedArticle["author_id"].toString().toInt())
                softly.assertThat(actualCreatedArticle.favoritesCount)
                    .isEqualTo(expectedCreatedArticle["favorites_count"].toString().toInt())
                softly.assertThat(actualCreatedArticle.createdAt)
                    .isEqualTo(SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(expectedCreatedArticle["created_at"].toString()))
                softly.assertThat(actualCreatedArticle.updatedAt)
                    .isEqualTo(SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(expectedCreatedArticle["updated_at"].toString()))
                softly.assertThat(actualCreatedArticle.tagList)
                    .hasSameElementsAs(expectedCreatedArticle["tags"].toString().split(",").filter { it.isNotBlank() }.map { tag -> ArticleTag.newWithoutValidation(tag) })
                softly.assertAll()
            }

            assertThat(actualCreatedArticles.size)
                .`as`("レコード総数と一致する")
                .isEqualTo(expectedCreatedArticles.size)
        }
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class CreatedArticlesFromViewpointSet {
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
        fun `SeedData#createdArticlesFromViewpointSet()はDBのSeedDataを網羅している`() {
            /**
             * given:
             */

            /**
             * when:
             */
            val actualCreatedArticlesFromViewpointSet = SeedData.createdArticlesFromViewpointSet()

            /**
             * then:
             * - DBにあるお気に入り状態が一致する
             */
            actualCreatedArticlesFromViewpointSet.forEach { (userId, actualCreatedArticlesFromViewpoint) ->
                val expectedArticlesFromViewpoint = DbConnection.namedParameterJdbcTemplate.queryForList(
                    """
                        SELECT
                            articles.id
                            , (
                                SELECT
                                    CASE COUNT(favorites.id)
                                        WHEN 0 THEN '0'
                                        ELSE '1'
                                    END
                                FROM
                                    favorites
                                WHERE
                                    favorites.article_id = articles.id
                                    AND favorites.user_id = :viewpoint_user_id
                            ) AS favorited
                        FROM
                            articles
                        ;
                    """.trimIndent(),
                    MapSqlParameterSource()
                        .addValue("viewpoint_user_id", userId.value)
                )

                actualCreatedArticlesFromViewpoint.forEach { actualCreatedArticleFromViewpoint ->
                    val expectedArticleFromViewpoint = expectedArticlesFromViewpoint.find {
                        it["id"].toString().toInt() == actualCreatedArticleFromViewpoint.id.value
                    }!!
                    assertThat(actualCreatedArticleFromViewpoint.favorited)
                        .`as`("登録済みユーザー(id: ${userId.value})から見た、 作成済み記事(id: ${expectedArticleFromViewpoint["id"]})のお気に入り状態が一致する")
                        .isEqualTo(expectedArticleFromViewpoint["favorited"] == "1")
                }
            }
        }
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class Comments {
        @BeforeEach
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `SeedData#comments()はDBのSeedDataを網羅している`() {
            /**
             * given:
             */

            /**
             * when:
             */
            val actualCommentsSet = SeedData.comments()

            /**
             * then:
             * - コメントの内容がそれぞれ一致する
             * - レコードの総数が一致する
             */
            val expectedComments = DbConnection.namedParameterJdbcTemplate.queryForList(
                """
                    SELECT
                        id,
                        author_id,
                        article_id,
                        body
                    FROM
                        article_comments
                    ;
                """.trimIndent(),
                MapSqlParameterSource()
            )
            actualCommentsSet.forEach { (articleId, actualComments) ->
                actualComments.forEach { actualComment ->
                    val expectedComment =
                        expectedComments.find { it["id"].toString().toInt() == actualComment.id.value }!!

                    val softly = SoftAssertions()
                    softly.assertThat(actualComment.authorId.value)
                        .isEqualTo(expectedComment["author_id"].toString().toInt())
                    softly.assertThat(articleId.value).isEqualTo(expectedComment["article_id"].toString().toInt())
                    softly.assertThat(actualComment.body.value).isEqualTo(expectedComment["body"].toString())
                    softly.assertAll()
                }
            }

            assertThat(actualCommentsSet.values.sumOf { it.size })
                .`as`("レコード総数と一致する")
                .isEqualTo(expectedComments.size)
        }
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class OtherUsersFromViewpoint {
        @BeforeEach
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
            ]
        )
        fun `SeedData#otherUsersFromViewpointSet()はDBのSeedDataを網羅している`() {
            /**
             * given:
             */

            /**
             * when:
             */
            val actualOtherUsersFromViewpointSet = SeedData.otherUsersFromViewpointSet()

            /**
             * then:
             * - 特定の登録済みユーザーから見た登録済みユーザーのフォロー状態が一致する
             * - フォローレコードの総数が一致する
             */
            val expectedFollowings = DbConnection.namedParameterJdbcTemplate.queryForList(
                """
                    SELECT
                        following_id,
                        follower_id
                    FROM
                        followings
                    ;
                """.trimIndent(),
                MapSqlParameterSource()
            )
            actualOtherUsersFromViewpointSet.forEach { (userId, actualOtherUsers) ->
                val softly = SoftAssertions()
                actualOtherUsers.forEach { actualOtherUser ->
                    val following = expectedFollowings.find {
                        it["following_id"].toString().toInt() == actualOtherUser.userId.value &&
                            it["follower_id"].toString().toInt() == userId.value
                    }
                    when (actualOtherUser.following) {
                        false -> softly.assertThat(following)
                            .`as`("$userId は${actualOtherUser.userId}をフォローしていない")
                            .isNull()
                        true -> softly.assertThat(following)
                            .`as`("$userId は${actualOtherUser.userId}をフォローしている")
                            .isNotNull
                    }
                }
                softly.assertAll()
            }

            assertThat(actualOtherUsersFromViewpointSet.values.sumOf { it.filter { otherUser -> otherUser.following }.size })
                .`as`("followingがtrueである数は、followingsレコード数と一致する")
                .isEqualTo(expectedFollowings.size)
        }
    }
}
