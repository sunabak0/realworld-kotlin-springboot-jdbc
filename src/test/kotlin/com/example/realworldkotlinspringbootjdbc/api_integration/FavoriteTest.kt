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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

class FavoriteTest {
    @SpringBootTest
    @AutoConfigureMockMvc
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @Tag("ApiIntegration")
    class CreateArticleFavorite {

        @Autowired
        lateinit var mockMvc: MockMvc

        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/articles.yml",
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
            ]
        )
        @ExpectedDataSet(
            value = ["datasets/yml/then/article_repository/favorite-success.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `正常系-お気に入り未登録の作成済記事の slug を指定した場合、favorited=trueの作成済記事が戻り値`() {
            /**
             * given:
             * - 作成済記事が存在する slug
             * - 作成済記事をお気に入り未登録のユーザー（userId = 3）の sessionToken
             */
            val slug = "rust-vs-scala-vs-kotlin"
            val existedUser = SeedData.users().filter { it.userId.value == 3 }[0]
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/articles/$slug/favorite")
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
            val expectedResponseBody = """
                {
                  "article":
                     {
                        "title":"Rust vs Scala vs Kotlin",
                        "slug":"rust-vs-scala-vs-kotlin",
                        "body":"dummy-body",
                        "createdAt": "2022-01-01T00:00:00.000Z",
                        "updatedAt": "2022-01-01T00:00:00.000Z",
                        "description":"dummy-description",
                        "tagList": [
                            "rust",
                            "scala",
                            "kotlin"
                        ],
                        "favorited":true,
                        "favoritesCount":2,
                        "author": {
                            username: "paul-graham",
                            bio: "Lisper",
                            image: "",
                            following: true
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
                "datasets/yml/given/users.yml",
                "datasets/yml/given/tags.yml",
            ]
        )
        @ExpectedDataSet(
            value = ["datasets/yml/given/articles.yml", "datasets/yml/given/tags.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `正常系-お気に入り登録済の作成済記事の slug を指定した場合も、favorited=trueの作成済記事が戻り値`() {
            /**
             * given:
             * - 作成済記事が存在する slug
             * - 作成済記事をお気に入り登録済のユーザー（userId = 2）の sessionToken
             */
            val slug = "rust-vs-scala-vs-kotlin"
            val existedUser = SeedData.users().filter { it.userId.value == 2 }[0]
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/articles/$slug/favorite")
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
            val expectedResponseBody = """
                {
                  "article":
                     {
                        "title":"Rust vs Scala vs Kotlin",
                        "slug":"rust-vs-scala-vs-kotlin",
                        "body":"dummy-body",
                        "createdAt": "2022-01-01T00:00:00.000Z",
                        "updatedAt": "2022-01-01T00:00:00.000Z",
                        "description":"dummy-description",
                        "tagList": [
                            "rust",
                            "scala",
                            "kotlin"
                        ],
                        "favorited":true,
                        "favoritesCount":1,
                        "author": {
                            username: "paul-graham",
                            bio: "Lisper",
                            image: "",
                            following: true
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
                "datasets/yml/given/users.yml",
            ]
        )
        fun `準正常系-slug が無効な値の場合、「"slug が不正です"」が返される`() {
            /**
             * given:
             * - 有効でない slug（32文字より大きい）
             * - 作成済記事をお気に入り登録済のユーザー（userId = 2）の sessionToken
             */
            val slug = getRandomString(33)
            val existedUser = SeedData.users().filter { it.userId.value == 2 }[0]
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/articles/$slug/favorite")
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
            val expectedResponseBody = """
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

        @Test
        fun `準正常系-slug に該当する作成済記事がない場合、「"記事が見つかりませんでした"」が返される`() {
            /**
             * given:
             * - 有効でない slug（32文字より大きい）
             * - 作成済記事をお気に入り登録済のユーザー（userId = 2）の sessionToken
             */
            val slug = "dummy-slug"
            val existedUser = SeedData.users().filter { it.userId.value == 2 }[0]
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/articles/$slug/favorite")
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
            val expectedStatus = HttpStatus.NOT_FOUND.value()
            val expectedResponseBody = """
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
    }
}
