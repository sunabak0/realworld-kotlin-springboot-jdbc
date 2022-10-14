package com.example.realworldkotlinspringbootjdbc.api_integration.shared

import com.auth0.jwt.exceptions.JWTCreationException
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.presentation.shared.RealworldSessionEncodeErrorException
import com.example.realworldkotlinspringbootjdbc.util.MySession
import com.example.realworldkotlinspringbootjdbc.util.MySessionJwt
import com.fasterxml.jackson.annotation.JsonProperty
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.comparator.CustomComparator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class SharedRealworldExceptionController {
    @GetMapping("/throw_realworld_session_encode_error_exception")
    fun throwRealworldSessionEncodeErrorException(): ResponseEntity<String> =
        throw RealworldSessionEncodeErrorException(
            error = MySessionJwt.EncodeError.FailedEncode(
                cause = JWTCreationException(null, null),
                session = MySession(
                    userId = UserId(-1),
                    email = object : Email {
                        override val value: String get() = throw NotImplementedError()
                    }
                )
            )
        )

    data class DummyRequestBody(
        @field:Valid
        @field:JsonProperty("name", required = true) val name: String,
        @field:Valid
        @field:JsonProperty("age", required = true) val age: Int
    )
    @PostMapping("/throw_http_message_not_readable_exception")
    fun throwHttpMessageNotReadableException(
        @RequestBody @Valid requestBody: DummyRequestBody
    ): ResponseEntity<String> =
        throw UnsupportedOperationException("これがthrowされる前にHttpMessageNotReadableExceptionがthrowされる: $requestBody")
}

@SpringBootTest
@AutoConfigureMockMvc
class SharedRealworldExceptionHandlersTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `異常系-リクエストボディをclassにマッピングするまでに異常があった場合、400エラーレスポンスを返す`() {
        /**
         * given:
         * - DummyRequestBodyが要求している形にそぐわないJSON
         */
        val wrongRequestBody = """
            {
                "foo": "bar"
            }
        """.trimIndent()

        /**
         * when:
         */
        val response = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/throw_http_message_not_readable_exception")
                .contentType(MediaType.APPLICATION_JSON)
                .content(wrongRequestBody)
        ).andReturn().response
        val actualStatus = response.status
        val actualResponseBody = response.contentAsString

        /**
         * then:
         */
        val expectedStatus = 400
        val expectedResponseBody = """
            {
              "errors": {
                "body": ["リクエストボディが読み取れませんでした(要求しているJSONの形ではない可能性があります)"]
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
    fun `異常系-セッションエンコードに失敗したという旨の例外が投げられた時、500エラーレスポンスを返す`() {
        /**
         * given:
         */

        /**
         * when:
         */
        val response = mockMvc.get("/throw_realworld_session_encode_error_exception").andReturn().response
        val actualStatus = response.status
        val actualResponseBody = response.contentAsString

        /**
         * then:
         */
        val expectedStatus = 500
        val expectedResponseBody = """
            {
              "errors": {
                "body": ["想定外のエラーが起きました"]
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
