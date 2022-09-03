package com.example.realworldkotlinspringbootjdbc.api_integration

import com.example.realworldkotlinspringbootjdbc.infra.DbConnection
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.junit5.api.DBRider
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.util.MultiValueMapAdapter

class ArticleTest {
    @SpringBootTest
    @AutoConfigureMockMvc
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @Tag("ApiIntegration")
    class Filter {

        @Autowired
        lateinit var mockMvc: MockMvc

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
        fun `正常系-tag=タグ名でフィルタした場合、そのタグを持つ記事ひっかかる`() {
            /**
             * given:
             */
            val queryParameters = MultiValueMapAdapter(
                mapOf(
                    "tag" to listOf("kotlin")
                )
            )

            /**
             * when:
             */
            val actual = mockMvc.get("/articles") {
                contentType = MediaType.APPLICATION_JSON
                params = queryParameters
            }

            /**
             * then:
             * - 比較するのは日付以外全て
             */
            actual.andExpect { status { isOk() } }
                .andExpect {
                    content {
                        json(
                            """
                                {
                                    "articlesCount":2,
                                    "articles":[
                                        {
                                            "title":"Rust vs Scala vs Kotlin",
                                            "slug":"rust-vs-scala-vs-kotlin",
                                            "body":"dummy-body",
                                            "description":"dummy-description",
                                            "tagList":["rust","scala","kotlin"],
                                            "authorId":1,
                                            "favorited":false,
                                            "favoritesCount":1
                                        },
                                        {
                                            "title":"Functional programming kotlin",
                                            "slug":"functional-programming-kotlin",
                                            "body":"dummy-body",
                                            "description":"dummy-description",
                                            "tagList":["kotlin"],
                                            "authorId":1,
                                            "favorited":false,
                                            "favoritesCount":1
                                        }
                                    ]
                                }
                            """.trimIndent()
                        )
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
        fun `正常系-favorited=ユーザー名の場合、指定されたユーザーがお気に入りの記事がひっかかる`() {
            /**
             * given:
             */
            val favoritedFilterParameter = "favorited=松本行弘"

            /**
             * when:
             */
            val result = mockMvc.get("/articles?$favoritedFilterParameter")

            /**
             * then:
             */
            result.andExpect { status { isOk() } }
                .andExpect {
                    content {
                        json(
                            """
                            {
                                "articlesCount":2,
                                "articles":[
                                    {
                                        "title":"Rust vs Scala vs Kotlin",
                                        "slug":"rust-vs-scala-vs-kotlin",
                                        "body":"dummy-body",
                                        "description":"dummy-description",
                                        "tagList":["rust","scala","kotlin"],
                                        "authorId":1,
                                        "favorited":false,
                                        "favoritesCount":1
                                    },
                                    {
                                        "title":"TDD(Type Driven Development)",
                                        "slug":"tdd-type-driven-development",
                                        "body":"dummy-body",
                                        "description":"dummy-description",
                                        "tagList":[""],
                                        "authorId":2,
                                        "favorited":false,
                                        "favoritesCount":2
                                    }
                                ]
                            }
                            """.trimIndent()
                        )
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
        fun `正常系-author=ユーザー名の場合、指定されたユーザーが著者である記事がひっかかる`() {
            /**
             * given:
             */

            /**
             * when:
             */
            val actual = mockMvc.get("/articles?favorited=graydon-hoare")

            /**
             * then:
             */
            actual.andExpect { status { isOk() } }
                .andExpect {
                    content {
                        json(
                            """
                            {
                                "articlesCount":1,
                                "articles":[
                                    {
                                        "title":"Functional programming kotlin",
                                        "slug":"functional-programming-kotlin",
                                        "body":"dummy-body",
                                        "description":"dummy-description",
                                        "tagList":["kotlin"],
                                        "authorId":1,
                                        "favorited":false,
                                        "favoritesCount":1
                                    }
                                ]
                            }
                            """.trimIndent()
                        )
                    }
                }
        }

        @Disabled
        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `正常系-favorited=ユーザー名とauthor=ユーザー名の場合、両方の条件にANDでひっかかる`() {
            TODO()
        }

        @Disabled
        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `正常系-author=ユーザー名とtag=ユーザー名の場合、両方の条件にANDでひっかかる`() {
            TODO()
        }

        @Disabled
        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `正常系-limitに余裕があっても、最大limit分だけ取得する`() {
            TODO()
        }

        @Disabled
        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `正常系-フィルタ結果から、0番目から数えてoffset番目から最大limit分だけ取得する`() {
            TODO()
        }
    }
}
