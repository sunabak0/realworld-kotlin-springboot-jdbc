package com.example.realworldkotlinspringbootjdbc.api_integration

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.infra.DbConnection
import com.example.realworldkotlinspringbootjdbc.util.MySession
import com.example.realworldkotlinspringbootjdbc.util.MySessionJwt
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.github.database.rider.junit5.api.DBRider
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

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
             * - ステータスコードが 200
             * - 作成日時と更新日時以外を比較する
             */
            val expected =
                """
                    {
                      "comments": [
                        {
                          "id": 1,
                          "body": "dummy-comment-body-01",
                          "createdAt": "2022-09-26T15:00:00.000Z",
                          "updatedAt": "2022-09-26T15:00:00.000Z",
                          "authorId": 3
                        },
                        {
                          "id": 3,
                          "body": "dummy-comment-body-03",
                          "createdAt": "2022-09-26T15:00:00.000Z",
                          "updatedAt": "2022-09-26T15:00:00.000Z",
                          "authorId": 2
                        },
                        {
                          "id": 5,
                          "body": "dummy-comment-body-02",
                          "createdAt": "2022-09-26T15:00:00.000Z",
                          "updatedAt": "2022-09-26T15:00:00.000Z",
                          "authorId": 3
                        }
                      ]
                    }
                """.trimIndent()
            val actualResponseBody = actual.andExpect { status { isOk() } }.andReturn().response.contentAsString
            JSONAssert.assertEquals(
                expected,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.STRICT,
                    Customization("*.createdAt") { _, _ -> true },
                    Customization("*.updatedAt") { _, _ -> true },
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
            val actual = mockMvc.get("/articles/$pathParameter/comments") {
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
            val pathParameter = "fake"

            /**
             * when:
             */
            val actual = mockMvc.get("/articles/$pathParameter/comments") {
                contentType = MediaType.APPLICATION_JSON
            }

            /**
             * then:
             * - ステータスコードが 404
             */
            val expected =
                """
                    {
                        "errors":{"body":["記事が見つかりませんでした"]}
                    }
                """.trimIndent()
            val actualResponseBody = actual.andExpect { status { isNotFound() } }.andReturn().response.contentAsString
            JSONAssert.assertEquals(
                expected,
                actualResponseBody,
                CustomComparator(JSONCompareMode.STRICT)
            )
        }

        @Test
        fun `準正常系-slug が無効な値の場合、NotFoundError が返される`() {
            /**
             * given:
             * - null の場合
             */
            val pathParameter = ""

            /**
             * when:
             */
            val actual = mockMvc.get("/articles/$pathParameter/comments") {
                contentType = MediaType.APPLICATION_JSON
            }

            /**
             * then:
             * - ステータスコードが 404
             */
            val expected =
                """
                    {
                        "errors":{"body":["記事が見つかりませんでした"]}
                    }
                """.trimIndent()
            val actualResponseBody = actual.andExpect { status { isNotFound() } }.andReturn().response.contentAsString
            JSONAssert.assertEquals(
                expected,
                actualResponseBody,
                CustomComparator(JSONCompareMode.STRICT)
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
             * - userId = 1、email = "paul-graham@example.com" の登録済ユーザーのログイン用 JWT を作成する
             */
            val registeredUser = RegisteredUser.newWithoutValidation(
                userId = UserId(1),
                email = Email.newWithoutValidation("paul-graham@example.com"),
                username = Username.newWithoutValidation("paul-graham"),
                bio = Bio.newWithoutValidation("Rustを作った"),
                image = Image.newWithoutValidation("")
            )
            val session = MySession(userId = registeredUser.userId, email = registeredUser.email)
            val token =
                JWT.create()
                    .withIssuer(MySessionJwt.ISSUER)
                    .withClaim(MySessionJwt.USER_ID_KEY, session.userId.value)
                    .withClaim(MySessionJwt.EMAIL_KEY, session.email.value)
                    .sign(Algorithm.HMAC256("secret"))
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
            val actual = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/articles/$pathParameter/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawRequestBody)
                    .header("Authorization", token)
            )

            /**
             * then:
             * - ステータスコードが 200
             * - createdAt、updatedAt はメタデータなので比較しない
             */
            val expected =
                """
                    {
                      "Comment": {
                        "id": 10001,
                        "body": "created-dummy-body-1",
                        "createdAt": "2022-09-26T15:00:00.000Z",
                        "updatedAt": "2022-09-26T15:00:00.000Z",
                        "authorId": 1
                      }
                    }
                """.trimIndent()
            val actualResponseBody = actual
                .andExpect(status().isOk)
                .andReturn().response.contentAsString
            JSONAssert.assertEquals(
                expected,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.STRICT,
                    Customization("Comment.createdAt") { _, _ -> true },
                    Customization("Comment.updatedAt") { _, _ -> true },
                )
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
            val registeredUser = RegisteredUser.newWithoutValidation(
                userId = UserId(3),
                email = Email.newWithoutValidation("graydon-hoare@example.com"),
                username = Username.newWithoutValidation("graydon-hoare"),
                bio = Bio.newWithoutValidation("Rustを作った"),
                image = Image.newWithoutValidation("")
            )
            val session = MySession(userId = registeredUser.userId, email = registeredUser.email)
            val token =
                JWT.create()
                    .withIssuer(MySessionJwt.ISSUER)
                    .withClaim(MySessionJwt.USER_ID_KEY, session.userId.value)
                    .withClaim(MySessionJwt.EMAIL_KEY, session.email.value)
                    .sign(Algorithm.HMAC256("secret"))
            val slug = "rust-vs-scala-vs-kotlin"
            val id = 1

            /**
             * when:
             */
            val actual = mockMvc.perform(
                MockMvcRequestBuilders
                    .delete("/articles/$slug/comments/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", token)
            )

            /**
             * then:
             */
            actual.andExpect(status().isOk)
                .andExpect(content().string(""))
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
            val registeredUser = RegisteredUser.newWithoutValidation(
                userId = UserId(1),
                email = Email.newWithoutValidation("paul-graham@example.com"),
                username = Username.newWithoutValidation("paul-graham"),
                bio = Bio.newWithoutValidation("Lisper"),
                image = Image.newWithoutValidation("")
            )
            val session = MySession(userId = registeredUser.userId, email = registeredUser.email)
            val token =
                JWT.create()
                    .withIssuer(MySessionJwt.ISSUER)
                    .withClaim(MySessionJwt.USER_ID_KEY, session.userId.value)
                    .withClaim(MySessionJwt.EMAIL_KEY, session.email.value)
                    .sign(Algorithm.HMAC256("secret"))
            val slug = "rust-vs-scala-vs-kotlin"
            val id = 1

            /**
             * when:
             */
            val actual = mockMvc.perform(
                MockMvcRequestBuilders
                    .delete("/articles/$slug/comments/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", token)
            )

            /**
             * then:
             * - ステータスコードが 401
             */
            val expected =
                """
                    {
                        "errors": {
                            "body": ["コメントの削除が許可されていません"]
                        }
                    }
                """.trimIndent()
            val actualResponseBody = actual.andExpect(status().isUnauthorized).andReturn().response.contentAsString
            JSONAssert.assertEquals(
                expected,
                actualResponseBody,
                CustomComparator(JSONCompareMode.STRICT)
            )
        }
    }
}
