package com.example.realworldkotlinspringbootjdbc.api_integration

import com.example.realworldkotlinspringbootjdbc.infra.DbConnection
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.junit5.api.DBRider
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

class CommentTest {
    @SpringBootTest
    @AutoConfigureMockMvc
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @Tag("ApiIntegration")
    class List {

        @Autowired
        lateinit var mockMvc: MockMvc

        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/articles.yml"
            ]
        )
        fun `正常系-slug で指定した作成済記事のコメントが存在する場合全て取得される`() {
            /**
             * given:
             */
            val pathParameter = "rust-vs-scala-vs-kotlin"

            /**
             * when:
             */
            val actual = mockMvc.get("/articles/$pathParameter/comments") {
                contentType = MediaType.APPLICATION_JSON
            }

            /**
             * then:
             * - 作成日時と更新日時以外を比較する
             */
            actual.andExpect { status { isOk() } }
                .andExpect {
                    content {
                        json(
                            """
                                {
                                  "comments": [
                                    {
                                      "id": 1,
                                      "body": "dummy-comment-body-01",
                                      "authorId": 3
                                    },
                                    {
                                      "id": 3,
                                      "body": "dummy-comment-body-03",
                                      "authorId": 2
                                    },
                                    {
                                      "id": 5,
                                      "body": "dummy-comment-body-02",
                                      "authorId": 3
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
                "datasets/yml/given/articles.yml"
            ]
        )
        fun `正常系-slug で指定した作成済記事のコメントが存在しない場合、コメントが 0 件で取得される`() {
            /**
             * given:
             */
            val pathParameter = "functional-programming-kotlin"

            /**
             * when:
             */
            val actual = mockMvc.get("/articles/$pathParameter/comments") {
                contentType = MediaType.APPLICATION_JSON
            }

            /**
             * then:
             */
            actual.andExpect { status { isOk() } }
                .andExpect {
                    content {
                        json(
                            """
                                {
                                  "comments": []
                                }
                            """.trimIndent()
                        )
                    }
                }
        }
    }
}
