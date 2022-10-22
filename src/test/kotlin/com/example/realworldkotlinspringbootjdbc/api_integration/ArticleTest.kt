package com.example.realworldkotlinspringbootjdbc.api_integration

import arrow.core.getOrHandle
import com.example.realworldkotlinspringbootjdbc.api_integration.helper.DatetimeVerificationHelper
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.infra.DbConnection
import com.example.realworldkotlinspringbootjdbc.infra.helper.SeedData
import com.example.realworldkotlinspringbootjdbc.util.MySession
import com.example.realworldkotlinspringbootjdbc.util.MySessionJwtImpl
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.skyscreamer.jsonassert.Customization
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.comparator.CustomComparator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.util.MultiValueMapAdapter
import java.util.stream.IntStream

class ArticleTest {
    @SpringBootTest
    @AutoConfigureMockMvc
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class GetArticles {

        @Autowired
        lateinit var mockMvc: MockMvc

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
        fun `正常系-tag=タグ名でフィルタした場合、そのタグを持つ記事ひっかかる`() {
            /**
             * given:
             */
            val queryParameters = mapOf(
                "tag" to listOf("kotlin")
            )

            /**
             * when:
             */
            val response = mockMvc.get("/articles") {
                contentType = MediaType.APPLICATION_JSON
                params = MultiValueMapAdapter(queryParameters)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                   "articlesCount":2,
                   "articles":[
                      {
                         "title":"Rust vs Scala vs Kotlin",
                         "slug":"rust-vs-scala-vs-kotlin",
                         "body":"dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description":"dummy-description",
                         "tagList":[
                            "rust",
                            "scala",
                            "kotlin"
                         ],
                         "authorId":1,
                         "favorited":false,
                         "favoritesCount":1
                      },
                      {
                         "title":"Functional programming kotlin",
                         "slug":"functional-programming-kotlin",
                         "body":"dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description":"dummy-description",
                         "tagList":[
                            "kotlin"
                         ],
                         "authorId":1,
                         "favorited":false,
                         "favoritesCount":1
                      }
                   ]
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("articles[*].createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualCreatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                    Customization("articles[*].updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualUpdatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                )
            )
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
            val queryParameters = mapOf(
                "favorited" to listOf("松本行弘")
            )

            /**
             * when:
             */
            val response = mockMvc.get("/articles") {
                contentType = MediaType.APPLICATION_JSON
                params = MultiValueMapAdapter(queryParameters)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                   "articlesCount":2,
                   "articles":[
                      {
                         "title":"Rust vs Scala vs Kotlin",
                         "slug":"rust-vs-scala-vs-kotlin",
                         "body":"dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description":"dummy-description",
                         "tagList":[
                            "rust",
                            "scala",
                            "kotlin"
                         ],
                         "authorId":1,
                         "favorited":false,
                         "favoritesCount":1
                      },
                      {
                         "title":"TDD(Type Driven Development)",
                         "slug":"tdd-type-driven-development",
                         "body":"dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description":"dummy-description",
                         "tagList":[],
                         "authorId":2,
                         "favorited":false,
                         "favoritesCount":2
                      }
                   ]
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("articles[*].createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualCreatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                    Customization("articles[*].updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualUpdatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                )
            )
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
            val queryParameters = mapOf(
                "author" to listOf("paul-graham")
            )

            /**
             * when:
             */
            val response = mockMvc.get("/articles") {
                contentType = MediaType.APPLICATION_JSON
                params = MultiValueMapAdapter(queryParameters)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                   "articlesCount":2,
                   "articles":[
                      {
                         "title":"Rust vs Scala vs Kotlin",
                         "slug":"rust-vs-scala-vs-kotlin",
                         "body":"dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description":"dummy-description",
                         "tagList":[
                            "rust",
                            "scala",
                            "kotlin"
                         ],
                         "authorId":1,
                         "favorited":false,
                         "favoritesCount":1
                      },
                      {
                         "title":"Functional programming kotlin",
                         "slug":"functional-programming-kotlin",
                         "body":"dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description":"dummy-description",
                         "tagList":[
                            "kotlin"
                         ],
                         "authorId":1,
                         "favorited":false,
                         "favoritesCount":1
                      }
                   ]
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("articles[*].createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualCreatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                    Customization("articles[*].updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualUpdatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                )
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `正常系-favorited=ユーザー名とauthor=ユーザー名の場合、両方の条件にANDでひっかかる`() {
            /**
             * given:
             * - 著者が異なる2つの作成済み記事をお気に入りしているユーザー名
             * - 上記の作成済み記事の著者1人のユーザー名
             */
            val queryParameters = mapOf(
                "favorited" to listOf("松本行弘"),
                "author" to listOf("paul-graham")
            )

            /**
             * when:
             */
            val response = mockMvc.get("/articles") {
                contentType = MediaType.APPLICATION_JSON
                params = MultiValueMapAdapter(queryParameters)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             *   - 引っかかるのは1つのみ
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                   "articlesCount": 1,
                   "articles": [
                      {
                         "title": "Rust vs Scala vs Kotlin",
                         "slug": "rust-vs-scala-vs-kotlin",
                         "body": "dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description": "dummy-description",
                         "tagList": [
                            "rust",
                            "scala",
                            "kotlin"
                         ],
                         "authorId": 1,
                         "favorited": false,
                         "favoritesCount": 1
                      }
                   ]
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("articles[*].createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualCreatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                    Customization("articles[*].updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualUpdatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                )
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `正常系-author=ユーザー名とtag=タグの場合、両方の条件にANDでひっかかる`() {
            /**
             * given:
             * - 2つの作成済み記事の著者のユーザー名
             * - 上記の内、片方にしか無いタグ
             */
            val queryParameters = mapOf(
                "author" to listOf("paul-graham"),
                "tag" to listOf("rust")
            )

            /**
             * when:
             */
            val response = mockMvc.get("/articles") {
                contentType = MediaType.APPLICATION_JSON
                params = MultiValueMapAdapter(queryParameters)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             *   - 引っかかるのは1つのみ
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                   "articlesCount": 1,
                   "articles": [
                      {
                         "title": "Rust vs Scala vs Kotlin",
                         "slug": "rust-vs-scala-vs-kotlin",
                         "body": "dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description": "dummy-description",
                         "tagList": [
                            "rust",
                            "scala",
                            "kotlin"
                         ],
                         "authorId": 1,
                         "favorited": false,
                         "favoritesCount": 1
                      }
                   ]
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("articles[*].createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualCreatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                    Customization("articles[*].updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualUpdatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                )
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `正常系-著者名に存在しないユーザー名を指定した場合、1つもひっかからない`() {
            /**
             * given:
             * - 存在しないユーザー名
             */
            val queryParameters = mapOf(
                "author" to listOf("not-existed-username"),
            )

            /**
             * when:
             */
            val response = mockMvc.get("/articles") {
                contentType = MediaType.APPLICATION_JSON
                params = MultiValueMapAdapter(queryParameters)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                   "articlesCount": 0,
                   "articles": []
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(JSONCompareMode.NON_EXTENSIBLE)
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `正常系-どっちかだけにしかひっかからないフィルタパラメータを指定した場合、(ANDフィルタなので)1つもひっかからない`() {
            /**
             * given:
             * - 存在しないユーザー名
             */
            val queryParameters = mapOf(
                "author" to listOf("松本行弘"),
                "tag" to listOf("lisp")
            )

            /**
             * when:
             */
            val response = mockMvc.get("/articles") {
                contentType = MediaType.APPLICATION_JSON
                params = MultiValueMapAdapter(queryParameters)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                   "articlesCount": 0,
                   "articles": []
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(JSONCompareMode.NON_EXTENSIBLE)
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `正常系-limitに余裕があっても、引っかかった数からoffset値が適用されただけ取得する`() {
            /**
             * given:
             * - offsetは1
             * - offsetが0の場合でも、余裕があるlimit(記事は全部で3つ)
             */
            val queryParameters = mapOf(
                "offset" to listOf("1"),
                "limit" to listOf("100")
            )

            /**
             * when:
             */
            val response = mockMvc.get("/articles") {
                contentType = MediaType.APPLICATION_JSON
                params = MultiValueMapAdapter(queryParameters)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             *   - 引っかかるのはoffset値を適用した2つのみ(limitに余裕があっても、3つ返したりはしない)
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                   "articlesCount": 3,
                   "articles":[
                      {
                         "title": "Functional programming kotlin",
                         "slug": "functional-programming-kotlin",
                         "body": "dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description": "dummy-description",
                         "tagList": [
                            "kotlin"
                         ],
                         "authorId": 1,
                         "favorited": false,
                         "favoritesCount": 1
                      },
                      {
                         "title": "TDD(Type Driven Development)",
                         "slug": "tdd-type-driven-development",
                         "body": "dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description": "dummy-description",
                         "tagList": [],
                         "authorId": 2,
                         "favorited": false,
                         "favoritesCount": 2
                      }
                   ]
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("articles[*].createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualCreatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                    Customization("articles[*].updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualUpdatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                )
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `正常系-フィルタ結果から、0番目から数えてoffset番目から最大limit分だけ取得する`() {
            /**
             * given:
             */
            val queryParameters = mapOf(
                "offset" to listOf("1"),
                "limit" to listOf("1")
            )

            /**
             * when:
             */
            val response = mockMvc.get("/articles") {
                contentType = MediaType.APPLICATION_JSON
                params = MultiValueMapAdapter(queryParameters)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                   "articlesCount": 3,
                   "articles":[
                      {
                         "title": "Functional programming kotlin",
                         "slug": "functional-programming-kotlin",
                         "body": "dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description": "dummy-description",
                         "tagList": [
                            "kotlin"
                         ],
                         "authorId": 1,
                         "favorited": false,
                         "favoritesCount": 1
                      }
                   ]
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("articles[*].createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualCreatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                    Customization("articles[*].updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualUpdatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                )
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `準正常系-バリデーションエラーを起こすようなフィルタパラメータだった場合、フィルタに失敗する`() {
            /**
             * given:
             * - 負の値であるoffset
             * - 数値に変換できないlimit
             */
            val queryParameters = mapOf(
                "offset" to listOf("-1"),
                "limit" to listOf("数値に変換できない")
            )

            /**
             * when:
             */
            val response = mockMvc.get("/articles") {
                contentType = MediaType.APPLICATION_JSON
                params = MultiValueMapAdapter(queryParameters)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 422
            val expectedResponseBody = """
                {
                   "errors":{
                      "body":[
                         {
                            "value": "数値に変換できない",
                            "key": "LimitError",
                            "message": "数値に変換できる数字にしてください"
                         },
                         {
                            "value": -1,
                            "key": "LimitError",
                            "message": "0以上である必要があります"
                         }
                      ]
                   }
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(JSONCompareMode.NON_EXTENSIBLE)
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `準正常系-Offset値がフィルタ後の作成済み記事の数を超えている場合、フィルタに失敗する`() {
            /**
             * given:
             */
            val queryParameters = mapOf(
                "offset" to listOf("100"),
            )

            /**
             * when:
             */
            val response = mockMvc.get("/articles") {
                contentType = MediaType.APPLICATION_JSON
                params = MultiValueMapAdapter(queryParameters)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 422
            val expectedResponseBody = """
                {
                   "errors": {
                      "body": [
                        "offset値がフィルタした結果の作成済み記事の数を超えています(offset=100, articlesCount=3)"
                      ]
                   }
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(JSONCompareMode.NON_EXTENSIBLE)
            )
        }
        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `正常系-ログイン済みの場合、そのログイン済みユーザーから見た、作成済み記事に対してお気に入り情報と著者のフォロー情報がレスポンスに載る`() {
            /**
             * given:
             * - SeedDataの1番目のユーザー(user2)
             */
            val existedUser = SeedData.users().toList()[1]
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }

            /**
             * when:
             * - フィルタパラメータは無し
             */
            val response = mockMvc.get("/articles") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", sessionToken)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             *   - ログイン済みのユーザー観点からの作成済み記事のお気に入り情報が載っている
             *   - ログイン済みのユーザー観点からの作成済み記事の著者のフォロー情報が載っている
             * - user2
             *   - フォロー
             *     - user1: 未フォロー
             *     - user3: フォロー済み
             *   - お気に入り
             *     - 記事1: お気に入り済み
             *     - 記事2: お気に入りではない
             *     - 記事3: お気に入り済み
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                   "articlesCount": 3,
                   "articles": [
                      {
                         "title": "Rust vs Scala vs Kotlin",
                         "slug": "rust-vs-scala-vs-kotlin",
                         "body": "dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description": "dummy-description",
                         "tagList":[
                            "rust",
                            "scala",
                            "kotlin"
                         ],
                         "authorId": 1,
                         "favorited": true,
                         "favoritesCount": 1
                      },
                      {
                         "title": "Functional programming kotlin",
                         "slug": "functional-programming-kotlin",
                         "body": "dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description": "dummy-description",
                         "tagList":[
                            "kotlin"
                         ],
                         "authorId": 1,
                         "favorited": false,
                         "favoritesCount": 1
                      },
                      {
                         "title": "TDD(Type Driven Development)",
                         "slug": "tdd-type-driven-development",
                         "body": "dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description": "dummy-description",
                         "tagList": [],
                         "authorId": 2,
                         "favorited": true,
                         "favoritesCount": 2
                      }
                   ]
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("articles[*].createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualCreatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                    Customization("articles[*].updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualUpdatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                )
            )
        }
    }

    @SpringBootTest
    @AutoConfigureMockMvc
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class CreateArticle {
        @Autowired
        lateinit var mockMvc: MockMvc

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
            value = ["datasets/yml/then/article_repository/create-success-case-of-partially-duplicated-tag-list.yml"],
            ignoreCols = ["id", "slug", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `正常系-記事の項目が全て有効な場合、記事の作成に成功する`() {
            /**
             * given:
             */
            val existedUser = SeedData.users().first()
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val requestBody = """
                {
                  "article": {
                    "title": "Comparing JVM lang",
                    "description": "JVM",
                    "body": "",
                    "tagList": ["kotlin", "clojure", "scala", "groovy"]
                  }
                }
            """.trimIndent()

            /**
             * when:
             */
            val response = mockMvc.post("/articles") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody
                header("Authorization", sessionToken)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                   "article":{
                      "title":"Comparing JVM lang",
                      "slug":"毎回変わるので、dummy-slug",
                      "body":"",
                      "createdAt":"2022-01-01T00:00:00.000Z",
                      "updatedAt":"2022-01-01T00:00:00.000Z",
                      "description":"JVM",
                      "tagList":[
                         "kotlin",
                         "clojure",
                         "scala",
                         "groovy"
                      ],
                      "authorId":1,
                      "favorited":false,
                      "favoritesCount":0
                   }
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("article.slug") { actualSlug, expectedDummy ->
                        Slug.new(actualSlug.toString()).isValid && expectedDummy == "毎回変わるので、dummy-slug"
                    },
                    Customization("article.createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualCreatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                    Customization("article.updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualUpdatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                )
            )
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
            value = ["datasets/yml/given/articles.yml"],
            ignoreCols = ["created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `準正常系-記事の項目にバリデーションエラーがある場合、記事の作成に失敗する`() {
            /**
             * given:
             */
            val existedUser = SeedData.users().first()
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val requestBody = """
                {
                  "article": {
                    "title": "${IntStream.range(0, 10).mapToObj { "長すぎるタイトル" }.toList().joinToString()}",
                    "description": "${IntStream.range(0, 70).mapToObj { "長すぎる概要" }.toList().joinToString()}",
                    "body": "${IntStream.range(0, 200).mapToObj { "長すぎる内容" }.toList().joinToString()}",
                    "tagList": ["too-long-too-long-tag"]
                  }
                }
            """.trimIndent()

            /**
             * when:
             */
            val response = mockMvc.post("/articles") {
                contentType = MediaType.APPLICATION_JSON
                content = requestBody
                header("Authorization", sessionToken)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 422
            val expectedResponseBody = """
                {
                   "errors":{
                      "body":[
                         {
                            "title":"長すぎるタイトル, 長すぎるタイトル, 長すぎるタイトル, 長すぎるタイトル, 長すぎるタイトル, 長すぎるタイトル, 長すぎるタイトル, 長すぎるタイトル, 長すぎるタイトル, 長すぎるタイトル",
                            "key":"Title",
                            "message":"titleは32文字以下にしてください。"
                         },
                         {
                            "description":"長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要, 長すぎる概要",
                            "key":"Description",
                            "message":"descriptionは64文字以下にしてください。"
                         },
                         {
                            "body":"長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容, 長すぎる内容",
                            "key":"Body",
                            "message":"body は1024文字以下にしてください。"
                         },
                         {
                            "tag":"too-long-too-long-tag",
                            "key":"Tag",
                            "message":"tagは16文字以下にしてください。"
                         }
                      ]
                   }
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("article.slug") { actualSlug, expectedDummy ->
                        Slug.new(actualSlug.toString()).isValid && expectedDummy == "dummy-slug"
                    },
                    Customization("article.createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualCreatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                    Customization("article.updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualUpdatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                )
            )
        }
    }

    @SpringBootTest
    @AutoConfigureMockMvc
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class GetArticlesFeed {

        @Autowired
        lateinit var mockMvc: MockMvc

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
        fun `正常系-ログイン済みの場合、フォロー済みのユーザーの最新作成済み記事の取得に成功する`() {
            /**
             * given:
             * - user3
             */
            val existedUser = SeedData.users().toList()[2]
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }

            /**
             * when:
             */
            val response = mockMvc.get("/articles/feed") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", sessionToken)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                   "articlesCount":2,
                   "articles":[
                      {
                         "title":"Functional programming kotlin",
                         "slug":"functional-programming-kotlin",
                         "body":"dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description":"dummy-description",
                         "tagList":[
                            "kotlin"
                         ],
                         "authorId":1,
                         "favorited":true,
                         "favoritesCount":1
                      },
                      {
                         "title":"TDD(Type Driven Development)",
                         "slug":"tdd-type-driven-development",
                         "body":"dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description":"dummy-description",
                         "tagList":[],
                         "authorId":2,
                         "favorited":false,
                         "favoritesCount":2
                      }
                   ]
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("articles[*].createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualCreatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                    Customization("articles[*].updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualUpdatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                )
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `正常系-limitを指定した場合、1度に取得する最大数はlimitまでになる`() {
            /**
             * given:
             * - limitに1を指定する
             */
            val existedUser = SeedData.users().toList()[2]
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val queryParameters = mapOf(
                "limit" to listOf("1")
            )

            /**
             * when:
             */
            val response = mockMvc.get("/articles/feed") {
                contentType = MediaType.APPLICATION_JSON
                params = MultiValueMapAdapter(queryParameters)
                header("Authorization", sessionToken)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                   "articlesCount":2,
                   "articles":[
                      {
                         "title":"Functional programming kotlin",
                         "slug":"functional-programming-kotlin",
                         "body":"dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description":"dummy-description",
                         "tagList":[
                            "kotlin"
                         ],
                         "authorId":1,
                         "favorited":true,
                         "favoritesCount":1
                      }
                   ]
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("articles[*].createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualCreatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                    Customization("articles[*].updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualUpdatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                )
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `正常系-offsetを指定した場合、0から数えてoffeset個めから取得する`() {
            /**
             * given:
             * - offsetに1を指定する
             */
            val existedUser = SeedData.users().toList()[2]
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val queryParameters = mapOf(
                "offset" to listOf("1")
            )

            /**
             * when:
             */
            val response = mockMvc.get("/articles/feed") {
                contentType = MediaType.APPLICATION_JSON
                params = MultiValueMapAdapter(queryParameters)
                header("Authorization", sessionToken)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                   "articlesCount":2,
                   "articles":[
                      {
                         "title":"TDD(Type Driven Development)",
                         "slug":"tdd-type-driven-development",
                         "body":"dummy-body",
                         "createdAt": "2022-01-01T00:00:00.000Z",
                         "updatedAt": "2022-01-01T00:00:00.000Z",
                         "description":"dummy-description",
                         "tagList":[],
                         "authorId":2,
                         "favorited":false,
                         "favoritesCount":2
                      }
                   ]
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("articles[*].createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualCreatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                    Customization("articles[*].updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualUpdatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                )
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `準常系-パラメータがバリデーションエラーを起こす場合、feed取得に失敗する`() {
            /**
             * given:
             * - offsetは負の値
             * - limitは負の値
             */
            val existedUser = SeedData.users().toList()[2]
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val queryParameters = mapOf(
                "offset" to listOf("-1"),
                "limit" to listOf("100000")
            )

            /**
             * when:
             */
            val response = mockMvc.get("/articles/feed") {
                contentType = MediaType.APPLICATION_JSON
                params = MultiValueMapAdapter(queryParameters)
                header("Authorization", sessionToken)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 422
            val expectedResponseBody = """
                {
                   "errors":{
                      "body":[
                         {
                            "value":100000,
                            "key":"LimitError",
                            "message":"100以下である必要があります"
                         },
                         {
                            "value":-1,
                            "key":"LimitError",
                            "message":"0以上である必要があります"
                         }
                      ]
                   }
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(JSONCompareMode.NON_EXTENSIBLE)
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `準正常系-Offset値が取得後の作成済み記事の数を超えている場合、feed取得に失敗する`() {
            /**
             * given:
             * - 取得後の作成済み記事の数を超えたoffset値
             */
            val existedUser = SeedData.users().toList()[2]
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val queryParameters = mapOf(
                "offset" to listOf("100"),
            )

            /**
             * when:
             */
            val response = mockMvc.get("/articles/feed") {
                contentType = MediaType.APPLICATION_JSON
                params = MultiValueMapAdapter(queryParameters)
                header("Authorization", sessionToken)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 422
            val expectedResponseBody = """
                {
                   "errors":{
                      "body":[
                         "offset値が作成済み記事の数を超えています(offset=100, articlesCount=2)"
                      ]
                   }
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(JSONCompareMode.NON_EXTENSIBLE)
            )
        }
    }

    @SpringBootTest
    @AutoConfigureMockMvc
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class GetArticle {

        @Autowired
        lateinit var mockMvc: MockMvc

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
        fun `正常系-存在するslugを指定した場合、作成済み記事の取得に成功する`() {
            /**
             * given:
             * - 存在するslug
             */
            val slug = "functional-programming-kotlin"

            /**
             * when:
             */
            val response = mockMvc.get("/articles/$slug") {
                contentType = MediaType.APPLICATION_JSON
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                   "article":{
                      "title":"Functional programming kotlin",
                      "slug":"functional-programming-kotlin",
                      "body":"dummy-body",
                      "createdAt": "2022-01-01T00:00:00.000Z",
                      "updatedAt": "2022-01-01T00:00:00.000Z",
                      "description":"dummy-description",
                      "tagList":["kotlin"],
                      "authorId":1,
                      "favorited":false,
                      "favoritesCount":1
                   }
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("article.createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualCreatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                    Customization("article.updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualUpdatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                )
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `正常系-ログイン済みの場合、作成済み記事に対してのお気に入り情報と著者に対してのフォロー情報がレスポンスボディに載る`() {
            /**
             * given:
             * - user3
             * - 存在するslug(article2)
             */
            val existedUser = SeedData.users().toList()[2]
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val slug = "functional-programming-kotlin"

            /**
             * when:
             */
            val response = mockMvc.get("/articles/$slug") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", sessionToken)
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             *   - user3は
             *     - article2をお気に入り済み
             *     - article2の著者をフォロー済み
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                   "article":{
                      "title":"Functional programming kotlin",
                      "slug":"functional-programming-kotlin",
                      "body":"dummy-body",
                      "createdAt": "2022-01-01T00:00:00.000Z",
                      "updatedAt": "2022-01-01T00:00:00.000Z",
                      "description":"dummy-description",
                      "tagList":["kotlin"],
                      "authorId":1,
                      "favorited":true,
                      "favoritesCount":1
                   }
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("article.createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualCreatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                    Customization("article.updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcAndParsable(
                            actualUpdatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                )
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `準正常系-slugがバリデーションエラーを起こす場合、見つからなかった旨のエラーレスポンスが返る`() {
            /**
             * given:
             * - バリデーションエラーを起こすslug
             */
            val slug = IntStream.range(0, 10).mapToObj { "長すぎるSlug" }.toList().joinToString()

            /**
             * when:
             */
            val response = mockMvc.get("/articles/$slug") {
                contentType = MediaType.APPLICATION_JSON
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 404
            val expectedResponseBody = """
                {"errors":{"body":["記事が見つかりませんでした"]}}
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(JSONCompareMode.NON_EXTENSIBLE)
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
                "datasets/yml/given/articles.yml",
            ]
        )
        fun `準正常系-存在しないslug指定した場合、見つからなかった旨のエラーレスポンスが返る`() {
            /**
             * given:
             * - 存在しないslug
             */
            val slug = "not-existed-slug"

            /**
             * when:
             */
            val response = mockMvc.get("/articles/$slug") {
                contentType = MediaType.APPLICATION_JSON
            }.andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 404
            val expectedResponseBody = """
                {"errors":{"body":["記事が見つかりませんでした"]}}
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(JSONCompareMode.NON_EXTENSIBLE)
            )
        }
    }
}
