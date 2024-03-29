package com.example.realworldkotlinspringbootjdbc.api_integration

import arrow.core.getOrHandle
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.infra.DbConnection
import com.example.realworldkotlinspringbootjdbc.infra.helper.SeedData
import com.example.realworldkotlinspringbootjdbc.util.MySession
import com.example.realworldkotlinspringbootjdbc.util.MySessionJwtImpl
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.util.stream.IntStream

class UserAndAuthenticationTest {
    @SpringBootTest
    @AutoConfigureMockMvc
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class CreateUser {
        @Autowired
        lateinit var mockMvc: MockMvc

        @BeforeEach
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml"
            ]
        )
        @ExpectedDataSet(
            value = ["datasets/yml/then/user_repository/register-success.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `正常系-ユーザー登録に成功`() {
            /**
             * given:
             * - 誰も使用していないEmail, ユーザー名
             */
            val requestBody = """
                {
                    "user": {
                        "email": "unregistered@example.com",
                        "password": "Passw0rd",
                        "username": "unregistered-username"
                    }
                }
            """.trimIndent()

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/users/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 201
            val expectedResponseBody = """
                {
                  "user": {
                    "email": "unregistered@example.com",
                    "username": "unregistered-username",
                    "image": "",
                    "bio": "",
                    "token": "dummy.jwt.JSONWebToken"
                  }
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("user.token") { actualToken, expectedToken ->
                        MySessionJwtImpl.decode(actualToken.toString()).isRight() &&
                            expectedToken == "dummy.jwt.JSONWebToken"
                    }
                )
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml"
            ]
        )
        @ExpectedDataSet(
            value = [
                "datasets/yml/given/users.yml"
            ],
            ignoreCols = ["created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `準正常系-Emailが登録済みの場合、ユーザー登録に失敗`() {
            /**
             * given:
             * - 既に利用されているユーザーのEmail
             */
            val requestBody = """
                {
                    "user": {
                        "email": "${SeedData.users().first().email.value}",
                        "password": "Passw0rd",
                        "username": "unregistered-username"
                    }
                }
            """.trimIndent()

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/users/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            ).andReturn().response
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
                    "body": ["メールアドレスは既に登録されています"]
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
                "datasets/yml/given/users.yml"
            ]
        )
        @ExpectedDataSet(
            value = [
                "datasets/yml/given/users.yml"
            ],
            ignoreCols = ["created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `準正常系-ユーザー名が登録済みの場合、ユーザー登録に失敗`() {
            /**
             * given:
             * - 既に利用されているユーザー名
             */
            val requestBody = """
                {
                    "user": {
                        "email": "unregistered@example.com",
                        "password": "Passw0rd",
                        "username": "${SeedData.users().first().username.value}"
                    }
                }
            """.trimIndent()

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/users/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            ).andReturn().response
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
                    "body": ["ユーザー名は既に登録されています"]
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
                "datasets/yml/given/users.yml"
            ]
        )
        @ExpectedDataSet(
            value = [
                "datasets/yml/given/users.yml"
            ],
            ignoreCols = ["created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `準正常系-バリデーションエラーの場合、ユーザー登録に失敗`() {
            /**
             * given:
             * - バリデーションエラーだらけのリクエストボディ
             */
            val requestBody = """
                {
                    "user": {
                        "email": "形式として間違っているEmail",
                        "password": "",
                        "username": "a"
                    }
                }
            """.trimIndent()

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/users/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する(配列の順番は厳密でなくてよい)
             */
            val expectedStatus = 422
            val expectedResponseBody = """
                {
                  "errors": {
                    "body": [
                      "メールアドレスが不正な形式です。(正しい形式例：john@example.com)",
                      "パスワードは8文字以上にしてください。",
                      "ユーザー名は4文字以上にしてください。"
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
    class Login {
        @Autowired
        lateinit var mockMvc: MockMvc

        @BeforeEach
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml"
            ]
        )
        fun `正常系-emailが存在しpassword正しい場合、ログインに成功`() {
            /**
             * given:
             * - 存在するemail
             * - 正しいpassword
             */
            val existedUser = SeedData.users().first()
            val requestBody = """
                {
                    "user": {
                        "email": "${existedUser.email.value}",
                        "password": "Passw0rd"
                    }
                }
            """.trimIndent()

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/users/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            ).andReturn().response
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
                  "user": {
                    "email": "${existedUser.email.value}",
                    "username": "${existedUser.username.value}",
                    "image": "${existedUser.image.value}",
                    "bio": "${existedUser.bio.value}",
                    "token": "dummy.jwt.JSONWebToken"
                  }
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("user.token") { actualToken, expectedToken ->
                        MySessionJwtImpl.decode(actualToken.toString()).isRight() &&
                            expectedToken == "dummy.jwt.JSONWebToken"
                    }
                )
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml"
            ]
        )
        fun `準正常系-passwordが誤っている場合、ログインに失敗`() {
            /**
             * given:
             * - 存在するemail
             * - 誤ったpassword
             */
            val existedUser = SeedData.users().first()
            val requestBody = """
                {
                    "user": {
                        "email": "${existedUser.email.value}",
                        "password": "wrong-password"
                    }
                }
            """.trimIndent()

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/users/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 401
            val expectedResponseBody = """
                {
                  "errors": {
                    "body": ["認証に失敗しました"]
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
                "datasets/yml/given/users.yml"
            ]
        )
        fun `準正常系-バリデーションエラーがある場合、ログインに失敗`() {
            /**
             * given:
             * - Emailとして間違っているemail
             */
            val requestBody = """
                {
                    "user": {
                        "email": "wrong-format",
                        "password": "fake-password"
                    }
                }
            """.trimIndent()

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/users/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 401
            val expectedResponseBody = """
                {
                  "errors": {
                    "body": ["メールアドレスが不正な形式です。(正しい形式例：john@example.com)"]
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
    class GetCurrentUser {
        @Autowired
        lateinit var mockMvc: MockMvc

        @BeforeEach
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml"
            ]
        )
        fun `正常系-セッションが有効な場合、ログイン済みの登録済みユーザー情報取得に成功`() {
            /**
             * given:
             * - 有効なセッション情報のToken(JWT)
             */
            val existedUser = SeedData.users().first()
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .get("/api/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", sessionToken)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             *   - Tokenはdecodeできる
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                  "user": {
                    "email": "${existedUser.email.value}",
                    "username": "${existedUser.username.value}",
                    "image": "${existedUser.image.value}",
                    "bio": "${existedUser.bio.value}",
                    "token": "dummy.jwt.JSONWebToken"
                  }
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("user.token") { actualToken, expectedToken ->
                        MySessionJwtImpl.decode(actualToken.toString()).isRight() &&
                            expectedToken == "dummy.jwt.JSONWebToken"
                    }
                )
            )
        }

        @Test
        fun `準正常系-セッショントークンがJWT形式でない場合、ログイン済みの登録済みユーザー情報取得に失敗`() {
            /**
             * given:
             * - decodeに失敗するトークン
             */
            val sessionToken = "NOT.JWT"

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .get("/api/user")
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
            val expectedStatus = 401
            val expectedResponseBody = "{}"

            assertThat(actualStatus).isEqualTo(expectedStatus)
            assertThat(actualResponseBody).isEqualTo(expectedResponseBody)
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml"
            ]
        )
        fun `準正常系-セッション情報のユーザーIDに該当する登録済みユーザーが存在しない場合、ログイン済みの登録済みユーザー情報取得に失敗`() {
            /**
             * given:
             * - 存在しないUserIdが埋め込まれたセッショントークン
             */
            val sessionToken = MySessionJwtImpl.encode(MySession(UserId(-1), SeedData.users().first().email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .get("/api/user")
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
            val expectedStatus = 401
            val expectedResponseBody = "{}"

            assertThat(actualStatus).isEqualTo(expectedStatus)
            assertThat(actualResponseBody).isEqualTo(expectedResponseBody)
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml"
            ]
        )
        fun `準正常系-セッション情報のEmailが異なった場合、ログイン済みの登録済みユーザー情報取得に失敗`() {
            /**
             * given:
             * - UserIdは存在するがEmailが古いなどで、DBのEmailと異なるEmailが埋め込まれたセッショントークン
             */
            val sessionToken = MySessionJwtImpl.encode(MySession(SeedData.users().first().userId, Email.newWithoutValidation("diff@example.com")))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .get("/api/user")
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
            val expectedStatus = 401
            val expectedResponseBody = "{}"

            assertThat(actualStatus).isEqualTo(expectedStatus)
            assertThat(actualResponseBody).isEqualTo(expectedResponseBody)
        }
    }

    @SpringBootTest
    @AutoConfigureMockMvc
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class UpdateCurrentUser {
        @Autowired
        lateinit var mockMvc: MockMvc

        @BeforeEach
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml"
            ]
        )
        @ExpectedDataSet(
            value = ["datasets/yml/then/user_repository/update-success-case-of-all-properties.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `正常系-更新する情報が有効な場合、ログイン中の登録済みユーザーの更新に成功`() {
            /**
             * given:
             * - 有効なセッション情報のToken(JWT)
             * - 有効なリクエストボディ(Emailもユーザー名も使われていない)
             */
            val existedUser = SeedData.users().first()
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val requestBody = """
                {
                    "user": {
                        "email": "new+${existedUser.email.value}",
                        "username": "new+${existedUser.username.value}",
                        "bio": "new+${existedUser.bio.value}",
                        "image": "new+${existedUser.image.value}"
                    }
                }
            """.trimIndent()

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .put("/api/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", sessionToken)
                    .content(requestBody)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             *   - Tokenはdecodeできる
             */
            val expectedStatus = 200
            val expectedResponseBody = """
                {
                  "user": {
                    "email": "new+${existedUser.email.value}",
                    "username": "new+${existedUser.username.value}",
                    "image": "new+${existedUser.image.value}",
                    "bio": "new+${existedUser.bio.value}",
                    "token": "dummy.jwt.JSONWebToken"
                  }
                }
            """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                CustomComparator(
                    JSONCompareMode.NON_EXTENSIBLE,
                    Customization("user.token") { actualToken, expectedToken ->
                        MySessionJwtImpl.decode(actualToken.toString()).isRight() &&
                            expectedToken == "dummy.jwt.JSONWebToken"
                    }
                )
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml"
            ]
        )
        @ExpectedDataSet(
            value = ["datasets/yml/given/users.yml"],
            ignoreCols = ["created_at", "updated_at"]
        )
        fun `準正常系-更新する情報が有効ではない場合、ログイン中の登録済みユーザーの更新に失敗`() {
            /**
             * given:
             * - 有効なセッション情報のToken(JWT)
             * - 有効ではないリクエストボディ
             */
            val existedUser = SeedData.users().first()
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val requestBody = """
                {
                    "user": {
                        "email": "Emailとして正しくないEmail",
                        "username": "長すぎるUsername長すぎるUsername長すぎるUsername長すぎるUsername長すぎるUsername長すぎるUsername長すぎるUsername長すぎるUsername",
                        "bio": "${IntStream.range(0, 100).mapToObj { "長すぎるBio" }.toList().joinToString()}",
                        "image": "${IntStream.range(0, 100).mapToObj { "長すぎるImage" }.toList().joinToString()}"
                    }
                }
            """.trimIndent()

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .put("/api/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", sessionToken)
                    .content(requestBody)
            ).andReturn().response
            val actualStatus = response.status
            val actualResponseBody = response.contentAsString

            /**
             * then:
             * - ステータスコードが一致する
             * - レスポンスボディが一致する
             */
            val expectedStatus = 400
            val expectedResponseBody = """
                {
                    "errors": {
                        "body": [
                            "メールアドレスが不正な形式です。(正しい形式例：john@example.com)",
                            "ユーザー名は32文字以下にしてください。",
                            "bioは512文字以下にしてください。",
                            "imageは512文字以下にしてください。"
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
                "datasets/yml/given/users.yml"
            ]
        )
        @ExpectedDataSet(
            value = ["datasets/yml/given/users.yml"],
            ignoreCols = ["created_at", "updated_at"]
        )
        fun `準正常系-更新しようとしたEmailが自分以外の誰かに利用されていた場合、ログイン中の登録済みユーザーの更新に失敗`() {
            /**
             * given:
             * - 有効なセッション情報のToken(JWT)
             * - 既に利用されているEmailに更新しようとしているリクエストボディ
             */
            val existedUser = SeedData.users().first()
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val requestBody = """
                {
                    "user": {
                        "email": "${SeedData.users().toList()[1].email.value}"
                    }
                }
            """.trimIndent()

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .put("/api/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", sessionToken)
                    .content(requestBody)
            ).andReturn().response
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
                        "body": ["メールアドレスは既に登録されています"]
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
                "datasets/yml/given/users.yml"
            ]
        )
        @ExpectedDataSet(
            value = ["datasets/yml/given/users.yml"],
            ignoreCols = ["created_at", "updated_at"]
        )
        fun `準正常系-更新しようとしたユーザー名が自分以外の誰かに利用されていた場合、ログイン中の登録済みユーザーの更新に失敗`() {
            /**
             * given:
             * - 有効なセッション情報のToken(JWT)
             * - 既に利用されているユーザー名に更新しようとしているリクエストボディ
             */
            val existedUser = SeedData.users().first()
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }
            val requestBody = """
                {
                    "user": {
                        "username": "${SeedData.users().toList()[1].username.value}"
                    }
                }
            """.trimIndent()

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .put("/api/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", sessionToken)
                    .content(requestBody)
            ).andReturn().response
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
                        "body": ["ユーザー名は既に登録されています"]
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
                "datasets/yml/given/users.yml"
            ]
        )
        @ExpectedDataSet(
            value = ["datasets/yml/given/users.yml"],
            ignoreCols = ["created_at", "updated_at"]
        )
        @Disabled
        fun `準正常系-セッション情報から取得したはずのユーザーが見つからない場合、ログイン中の登録済みユーザーの更新に失敗`() {
            /**
             * ほぼありえない状況
             * セッション情報からCurrentUserは見つかったが、DB検索時に見つからない状況
             *
             * カバレッジを埋めるためには、mockito等で調整する必要があります
             */
        }
    }
}
