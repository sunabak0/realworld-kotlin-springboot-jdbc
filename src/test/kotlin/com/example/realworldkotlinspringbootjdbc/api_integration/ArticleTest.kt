package com.example.realworldkotlinspringbootjdbc.api_integration

import arrow.core.getOrHandle
import com.example.realworldkotlinspringbootjdbc.api_integration.helper.DatetimeVerificationHelper
import com.example.realworldkotlinspringbootjdbc.infra.DbConnection
import com.example.realworldkotlinspringbootjdbc.infra.helper.SeedData
import com.example.realworldkotlinspringbootjdbc.util.MySession
import com.example.realworldkotlinspringbootjdbc.util.MySessionJwtImpl
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
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
                                        "tagList":[],
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
            val favoritedUsername = "松本行弘"
            val authorUsername = "paul-graham"

            /**
             * when:
             */
            val response = mockMvc.get("/articles?favorited=$favoritedUsername&author=$authorUsername").andReturn().response
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
            val authorUsername = "paul-graham"
            val tag = "rust"

            /**
             * when:
             */
            val response = mockMvc.get("/articles?author=$authorUsername&tag=$tag").andReturn().response
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
            val authorUsername = "not-existed-username"

            /**
             * when:
             */
            val response = mockMvc.get("/articles?author=$authorUsername").andReturn().response
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
            val authorUsername = "松本行弘"
            val tag = "lisp"

            /**
             * when:
             */
            val response = mockMvc.get("/articles?author=$authorUsername&tag=$tag").andReturn().response
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
            val offset = 1
            val limit = 100

            /**
             * when:
             */
            val response = mockMvc.get("/articles?offset=$offset&limit=$limit").andReturn().response
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
            val offset = 1
            val limit = 1

            /**
             * when:
             */
            val response = mockMvc.get("/articles?offset=$offset&limit=$limit").andReturn().response
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
            val invalidOffset = -1
            val invalidLimit = "数値に変換できない"

            /**
             * when:
             */
            val response = mockMvc.get("/articles?offset=$invalidOffset&limit=$invalidLimit").andReturn().response
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
            val overedOffset = 100

            /**
             * when:
             */
            val response = mockMvc.get("/articles?offset=$overedOffset").andReturn().response
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
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .get("/articles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", sessionToken)
            ).andReturn().response
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
}
