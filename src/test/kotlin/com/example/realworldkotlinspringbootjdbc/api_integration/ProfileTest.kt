package com.example.realworldkotlinspringbootjdbc.api_integration

import com.example.realworldkotlinspringbootjdbc.infra.DbConnection
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.junit5.api.DBRider
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
    }
}
