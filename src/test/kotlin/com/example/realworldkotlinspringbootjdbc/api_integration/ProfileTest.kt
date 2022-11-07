package com.example.realworldkotlinspringbootjdbc.api_integration

import arrow.core.getOrHandle
import com.example.realworldkotlinspringbootjdbc.infra.DbConnection
import com.example.realworldkotlinspringbootjdbc.infra.helper.SeedData
import com.example.realworldkotlinspringbootjdbc.util.MySession
import com.example.realworldkotlinspringbootjdbc.util.MySessionJwtImpl
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.junit5.api.DBRider
import net.bytebuddy.utility.RandomString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

class ProfileTest {
    @SpringBootTest
    @AutoConfigureMockMvc
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class GetProfileByUsername {
        @Autowired
        lateinit var mockMvc: MockMvc

        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml"
            ]
        )
        fun `正常系-未ログインで、username で指定した登録済ユーザーが存在する場合、Profile が取得できる`() {
            /**
             * given:
             * - 登録済ユーザーが存在する username
             */
            val username = "paul-graham"

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .get("/api/profiles/$username")
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
                      "profile": {
                        "username": "paul-graham",
                        "bio": "Lisper",
                        "image": "",
                        "following": false
                      }
                    }
                """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                JSONCompareMode.STRICT
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml"
            ]
        )
        fun `正常系-ログイン済で、username で指定した登録済ユーザーが存在する場合、Profile を取得できる。フォロイーの場合、following=trueになる`() {
            /**
             * given:
             * - 登録済ユーザーが存在する username
             * - username をフォロイーにもつユーザー
             */
            val username = "paul-graham"
            val existedUser = SeedData.users().filter { it.userId.value == 2 }[0]
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .get("/api/profiles/$username")
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
                      "profile": {
                        "username": "paul-graham",
                        "bio": "Lisper",
                        "image": "",
                        "following": true
                      }
                    }
                """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                JSONCompareMode.STRICT
            )
        }

        @Test
        fun `準正常系-Username が短すぎる場合、「ユーザー名は4文字以上にしてください」が返される`() {
            /**
             * given:
             * - 有効でない Username（短すぎる）
             */
            val username = "aaa"

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .get("/api/profiles/$username")
                    .contentType(MediaType.APPLICATION_JSON)
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
                        "body": ["ユーザー名は4文字以上にしてください。"]
                      }
                    }
                """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                JSONCompareMode.NON_EXTENSIBLE
            )
        }

        @Test
        fun `準正常系-Username が長すぎる場合、「ユーザー名は32文字以下にしてください」が返される`() {
            /**
             * given:
             * - 有効でない Username（長すぎる）
             */
            val username = RandomString(33)

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .get("/api/profiles/$username")
                    .contentType(MediaType.APPLICATION_JSON)
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
                        "body": ["ユーザー名は32文字以下にしてください。"]
                      }
                    }
                """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                JSONCompareMode.NON_EXTENSIBLE
            )
        }

        @Test
        fun `準正常系-Username に該当するユーザーが見つからなかった場合、「プロフィールが見つかりませんでした」が返される`() {
            /**
             * given:
             * - 有効な Username
             */
            val username = "dummy-username"

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .get("/api/profiles/$username")
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
                      "errors": {
                        "body": ["プロフィールが見つかりませんでした"]
                      }
                    }
                """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                JSONCompareMode.NON_EXTENSIBLE
            )
        }
    }

    @SpringBootTest
    @AutoConfigureMockMvc
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    class FollowUserByUsername {
        @Autowired
        lateinit var mockMvc: MockMvc

        @BeforeAll
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml"
            ]
        )
        fun `正常系-username で指定したユーザーが存在し、未フォローの場合、Profile が戻り値で following=true`() {
            /**
             * given:
             * - 登録済ユーザーが存在する username
             * - username がフォロイーでない、登録済ユーザーの sessionToken
             */
            val username = "松本行弘"
            val existedUser = SeedData.users().filter { it.userId.value == 1 }[0]
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/profiles/$username/follow")
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
                      "profile": {
                        "username": "松本行弘",
                        "bio": "Rubyを作った",
                        "image": "",
                        "following": true
                      }
                    }
                """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                JSONCompareMode.STRICT
            )
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/users.yml"
            ]
        )
        fun `正常系-username で指定したユーザーが存在し、フォロー済の場合、Profile が戻り値で following=true`() {
            /**
             * given:
             * - 登録済ユーザーが存在する username
             * - username がフォロイーである、登録済ユーザーの sessionToken
             */
            val username = "paul-graham"
            val existedUser = SeedData.users().filter { it.userId.value == 3 }[0]
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/profiles/$username/follow")
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
                      "profile": {
                        "username": "paul-graham",
                        "bio": "Lisper",
                        "image": "",
                        "following": true
                      }
                    }
                """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                JSONCompareMode.STRICT
            )
        }

        @Test
        fun `準正常系-Username が短すぎる場合、「ユーザー名は4文字以上にしてください」が返される`() {
            /**
             * given:
             * - 有効でない Username（短すぎる）
             * - 登録済ユーザーの sessionToken
             */
            val username = "aaa"
            val existedUser = SeedData.users().filter { it.userId.value == 3 }[0]
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/profiles/$username/follow")
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
                        "body": ["ユーザー名は4文字以上にしてください。"]
                      }
                    }
                """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                JSONCompareMode.NON_EXTENSIBLE
            )
        }

        @Test
        fun `準正常系-Username が長すぎる場合、「ユーザー名は32文字以下にしてください」が返される`() {
            /**
             * given:
             * - 有効でない Username（長すぎる）
             * - 登録済ユーザーの sessionToken
             */
            val username = RandomString(33)
            val existedUser = SeedData.users().filter { it.userId.value == 3 }[0]
            val sessionToken = MySessionJwtImpl.encode(MySession(existedUser.userId, existedUser.email))
                .getOrHandle { throw UnsupportedOperationException("セッションからJWTへの変換に失敗しました(前提条件であるため、元の実装を見直してください)") }

            /**
             * when:
             */
            val response = mockMvc.perform(
                MockMvcRequestBuilders
                    .post("/api/profiles/$username/follow")
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
                        "body": ["ユーザー名は32文字以下にしてください。"]
                      }
                    }
                """.trimIndent()
            assertThat(actualStatus).isEqualTo(expectedStatus)
            JSONAssert.assertEquals(
                expectedResponseBody,
                actualResponseBody,
                JSONCompareMode.NON_EXTENSIBLE
            )
        }
    }
}
