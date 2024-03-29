package com.example.realworldkotlinspringbootjdbc.api_integration

import arrow.core.getOrHandle
import com.example.realworldkotlinspringbootjdbc.api_integration.helper.DatetimeVerificationHelper
import com.example.realworldkotlinspringbootjdbc.api_integration.helper.GenerateRandomHelper.getRandomString
import com.example.realworldkotlinspringbootjdbc.infra.DbConnection
import com.example.realworldkotlinspringbootjdbc.infra.helper.SeedData
import com.example.realworldkotlinspringbootjdbc.util.MySession
import com.example.realworldkotlinspringbootjdbc.util.MySessionJwtImpl
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.util.stream.IntStream

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
                "datasets/yml/given/articles.yml",
                "datasets/yml/given/users.yml"
            ]
        )
        fun `正常系-未ログインで、slug で指定した作成済記事のコメントが存在する場合全て取得される`() {
            /**
             * given:
             * - 作成済記事が存在する slug
             */
            val slug = "rust-vs-scala-vs-kotlin"

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .get("/api/articles/$slug/comments")
                    .contentType(MediaType.APPLICATION_JSON)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = HttpStatus.OK.value()
            val expectedResponseBody =
                """
                    {
                      "comments": [
                        {
                          "id": 1,
                          "body": "dummy-comment-body-01",
                          "createdAt": "2022-01-01T00:00:00.000Z",
                          "updatedAt": "2022-01-01T00:00:00.000Z",
                          "author": {
                            "username": "graydon-hoare",
                            "bio": "Rustを作った",
                            image: "",
                            following: false
                          }
                        },
                        {
                          "id": 3,
                          "body": "dummy-comment-body-03",
                          "createdAt": "2022-01-01T00:00:00.000Z",
                          "updatedAt": "2022-01-01T00:00:00.000Z",
                          "author": {
                            "username": "松本行弘",
                            "bio": "Rubyを作った",
                            image: "",
                            following: false
                          }
                        },
                        {
                          "id": 5,
                          "body": "dummy-comment-body-02",
                          "createdAt": "2022-01-01T00:00:00.000Z",
                          "updatedAt": "2022-01-01T00:00:00.000Z",
                          "author": {
                            "username": "graydon-hoare",
                            "bio": "Rustを作った",
                            image: "",
                            following: false
                          }
                        }
                      ]
                    }
                """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.STRICT,
                    Customization("*.createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcWithoutMillisecondAndParsable(actualCreatedAt) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                    Customization("*.updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcWithoutMillisecondAndParsable(actualUpdatedAt) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                )
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/articles.yml",
                "datasets/yml/given/users.yml"
            ]
        )
        fun `正常系-ログイン状態で、slug で指定した作成済記事のコメントが存在する場合、コメントが全て取得され、author がフォロイーの場合、following キーのバリューが true になる`() {
            /**
             * given:
             * - フォロイーが存在するユーザー（userId = 3）の sessionToken
             * - 作成済記事が存在する slug
             */
            val existedUser = SeedData.users().filter { it.userId.value == 3 }[0]
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val slug = "rust-vs-scala-vs-kotlin"

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .get("/api/articles/$slug/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", sessionToken)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = HttpStatus.OK.value()
            val expectedResponseBody =
                """
                    {
                      "comments": [
                        {
                          "id": 1,
                          "body": "dummy-comment-body-01",
                          "createdAt": "2022-01-01T00:00:00.000Z",
                          "updatedAt": "2022-01-01T00:00:00.000Z",
                          "author": {
                            "username": "graydon-hoare",
                            "bio": "Rustを作った",
                            image: "",
                            following: false
                          }
                        },
                        {
                          "id": 3,
                          "body": "dummy-comment-body-03",
                          "createdAt": "2022-01-01T00:00:00.000Z",
                          "updatedAt": "2022-01-01T00:00:00.000Z",
                          "author": {
                            "username": "松本行弘",
                            "bio": "Rubyを作った",
                            image: "",
                            following: true
                          }
                        },
                        {
                          "id": 5,
                          "body": "dummy-comment-body-02",
                          "createdAt": "2022-01-01T00:00:00.000Z",
                          "updatedAt": "2022-01-01T00:00:00.000Z",
                          "author": {
                            "username": "graydon-hoare",
                            "bio": "Rustを作った",
                            image: "",
                            following: false
                          }
                        }
                      ]
                    }
                """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.STRICT,
                    Customization("*.createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcWithoutMillisecondAndParsable(actualCreatedAt) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                    Customization("*.updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcWithoutMillisecondAndParsable(actualUpdatedAt) && expectedDummy == "2022-01-01T00:00:00.000Z"
                    },
                )
            )
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
            val actual = mockMvc.get("/api/articles/$pathParameter/comments") {
                contentType = MediaType.APPLICATION_JSON
            }

            /**
             * then:
             */
            val expected =
                """
                    {
                      "comments": []
                    }
                """.trimIndent()
            val actualResponseBody = actual.andExpect { status { isOk() } }.andReturn().response.contentAsString
            JSONAssert.assertEquals(
                expected,
                actualResponseBody,
                CustomComparator(JSONCompareMode.STRICT)
            )
        }

        @Test
        fun `準正常系-slug で指定した作成済記事が存在しない場合、NotFoundError が返される`() {
            /**
             * given:
             */
            val slug = "fake"

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .get("/api/articles/$slug/comments")
                    .contentType(MediaType.APPLICATION_JSON)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = HttpStatus.NOT_FOUND.value()
            val expectedResponseBody =
                """
                    {
                        "errors":{"body":["記事が見つかりませんでした"]}
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
        fun `準正常系-slug が無効な値の場合、「"slug が不正です"」が返される`() {
            /**
             * given:
             * - 有効でない slug（32文字より大きい）
             */
            val pathParameter = getRandomString(33)

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .get("/api/articles/$pathParameter/comments")
                    .contentType(MediaType.APPLICATION_JSON)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             */
            val expectedStatus = HttpStatus.UNPROCESSABLE_ENTITY.value()
            val expectedResponseBody =
                """
                    {
                        "errors":{"body":["slug が不正です"]}
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
    @Tag("ApiIntegration")
    class Create {

        @Autowired
        lateinit var mockMvc: MockMvc

        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(value = ["datasets/yml/given/articles.yml", "datasets/yml/given/users.yml"])
        @ExpectedDataSet(
            value = ["datasets/yml/then/comment_repository/create-success.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `正常系-slug に該当する作成済記事が存在し、コメントの作成に成功する`() {
            /**
             * given:
             * - 有効なセッション情報のToken(JWT)
             * - 有効なパスパラメータ（Slug）
             * - 有効なリクエストボディ
             */
            val existedUser = SeedData.users().first()
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val pathParameter = "functional-programming-kotlin"
            val rawRequestBody = """
                {
                    "comment": {
                        "body": "created-dummy-body-1"
                    }
                }
            """.trimIndent()

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/articles/$pathParameter/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawRequestBody)
                    .header("Authorization", sessionToken)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = HttpStatus.OK.value()
            val expectedResponseBody =
                """
                    {
                      "comment": {
                        "id": 10001,
                        "body": "created-dummy-body-1",
                        "createdAt": "2022-01-01T00:00:00Z",
                        "updatedAt": "2022-01-01T00:00:00Z",
                        "author": {
                          "username": "paul-graham",
                          "bio": "Lisper",
                          "image": "",
                          "following": false
                        }
                      }
                    }
                """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.STRICT,
                    Customization("comment.createdAt") { actualCreatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcWithoutMillisecondAndParsable(
                            actualCreatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00Z"
                    },
                    Customization("comment.updatedAt") { actualUpdatedAt, expectedDummy ->
                        DatetimeVerificationHelper.expectIso8601UtcWithoutMillisecondAndParsable(
                            actualUpdatedAt
                        ) && expectedDummy == "2022-01-01T00:00:00Z"
                    },
                )
            )
        }

        @Test
        @DataSet(value = ["datasets/yml/given/users.yml"])
        fun `準正常系-slug に該当する作成済記事が存在しない場合、記事が見つからない`() {
            /**
             * given:
             * - 有効なセッション情報のToken(JWT)
             * - 有効なパスパラメータだが、存在しない Slug
             * - 有効なリクエストボディ
             */
            val existedUser = SeedData.users().first()
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val pathParameter = "dummy-slug"
            val rawRequestBody = """
                {
                    "comment": {
                        "body": "created-dummy-body-1"
                    }
                }
            """.trimIndent()

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/articles/$pathParameter/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawRequestBody)
                    .header("Authorization", sessionToken)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = HttpStatus.NOT_FOUND.value()
            val expectedResponseBody =
                """
                    {
                        "errors": {
                            "body": [
                                "記事が見つかりませんでした"
                            ]
                        } 
                    }
                """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                JSONCompareMode.NON_EXTENSIBLE,
            )
        }

        @Test
        fun `準正常系-作成するコメントが適切でない場合バリデーションエラー`() {
            /**
             * given:
             * - 有効なセッション情報のToken(JWT)
             * - 有効なパスパラメータ（Slug）
             * - 有効でないリクエストボディ
             */
            val existedUser = SeedData.users().first()
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val pathParameter = "dummy-slug"
            val rawRequestBody = """
                {
                    "comment": {
                        "body": "${IntStream.range(0, 1000).mapToObj { "長すぎるBody" }.toList().joinToString()}"
                    }
                }
            """.trimIndent()

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/articles/$pathParameter/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawRequestBody)
                    .header("Authorization", sessionToken)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = HttpStatus.UNPROCESSABLE_ENTITY.value()
            val expectedResponseBody =
                """
                    {
                        "errors": {
                            "body": [
                                "bodyは1024文字以下にしてください。"
                            ]
                        } 
                    }
                """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                JSONCompareMode.NON_EXTENSIBLE,
            )
        }

        @Test
        fun `準正常系-Slug が不正な場合バリデーションエラー`() {
            /**
             * given:
             * - 有効なセッション情報のToken(JWT)
             * - 有効でないパスパラメータ（Slug）
             * - 有効なリクエストボディ
             */
            val existedUser = SeedData.users().first()
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val pathParameter = IntStream.range(0, 100).mapToObj { "long-slug" }.toList().joinToString()
            val rawRequestBody = """
                {
                    "comment": {
                        "body": "created-dummy-body-1"
                    }
                }
            """.trimIndent()

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/articles/$pathParameter/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawRequestBody)
                    .header("Authorization", sessionToken)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = HttpStatus.UNPROCESSABLE_ENTITY.value()
            val expectedResponseBody =
                """
                    {
                        "errors": {
                            "body": [
                                "slug が不正です"
                            ]
                        } 
                    }
                """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                JSONCompareMode.NON_EXTENSIBLE,
            )
        }
    }

    @SpringBootTest
    @AutoConfigureMockMvc
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @Tag("ApiIntegration")
    class Delete {

        @Autowired
        lateinit var mockMvc: MockMvc

        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(value = ["datasets/yml/given/articles.yml", "datasets/yml/given/users.yml"])
        @ExpectedDataSet(
            value = ["datasets/yml/then/comment_repository/delete-success.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `正常系-slug に該当する作成済記事が存在し、コメント作成者が実行ユーザーだった場合、コメントの削除に成功する`() {
            /**
             * given:
             * - userId = 3, email = "graydon-hoare@example.com" の登録済ユーザーのログイン用 JWT を作成
             */
            val existedUser = SeedData.users().find { it.userId.value == 3 }!!
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val slug = "rust-vs-scala-vs-kotlin"
            val id = 1

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .delete("/api/articles/$slug/comments/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", sessionToken)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = HttpStatus.OK.value()
            val expectedResponseBody = ""

            assertThat(actualStatus).isEqualTo(expectedStatus)
            assertThat(actualResponseBody).isEqualTo(expectedResponseBody)
        }

        @Test
        @DataSet(value = ["datasets/yml/given/articles.yml", "datasets/yml/given/users.yml"])
        @ExpectedDataSet(
            value = ["datasets/yml/given/articles.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `準正常系-slug が不正な場合バリデーションエラー（「Slug が不正です」）が発生する`() {
            /**
             * given:
             * - userId = 1, email = "paul-graham@example.com" の登録済ユーザーのログイン用 JWT を作成
             */
            val existedUser = SeedData.users().find { it.userId.value == 1 }!!
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val slug = getRandomString(33)
            val id = 1

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .delete("/api/articles/$slug/comments/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", sessionToken)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = HttpStatus.UNPROCESSABLE_ENTITY.value()
            val expectedResponseBody =
                """
                    {
                        "errors": {
                            "body": ["Slug が不正です"]
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
        @DataSet(value = ["datasets/yml/given/articles.yml", "datasets/yml/given/users.yml"])
        @ExpectedDataSet(
            value = ["datasets/yml/given/articles.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `準正常系-id が不正だとバリデーションエラー（「コメント ID が不正です」）が発生する`() {
            /**
             * given:
             * - userId = 1, email = "paul-graham@example.com" の登録済ユーザーのログイン用 JWT を作成
             */
            val existedUser = SeedData.users().find { it.userId.value == 1 }!!
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val slug = "rust-vs-scala-vs-kotlin"
            val id = -1 // id は 1 以上の整数

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .delete("/api/articles/$slug/comments/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", sessionToken)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = HttpStatus.UNPROCESSABLE_ENTITY.value()
            val expectedResponseBody =
                """
                    {
                        "errors": {
                            "body": ["コメント ID が不正です"]
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
        @DataSet(value = ["datasets/yml/given/articles.yml", "datasets/yml/given/users.yml"])
        @ExpectedDataSet(
            value = ["datasets/yml/given/articles.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `準正常系-slug に該当する作成済記事が存在し、コメント作成者が実行ユーザーじゃない場合、コメントの削除に失敗し、認可エラーが発生する`() {
            /**
             * given:
             * - userId = 1, email = "paul-graham@example.com" の登録済ユーザーのログイン用 JWT を作成
             */
            val existedUser = SeedData.users().find { it.userId.value == 1 }!!
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val slug = "rust-vs-scala-vs-kotlin"
            val id = 1

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .delete("/api/articles/$slug/comments/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", sessionToken)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = HttpStatus.UNAUTHORIZED.value()
            val expectedResponseBody =
                """
                    {
                        "errors": {
                            "body": ["コメントの削除が許可されていません"]
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
}
