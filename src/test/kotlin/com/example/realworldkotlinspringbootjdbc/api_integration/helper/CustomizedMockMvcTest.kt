package com.example.realworldkotlinspringbootjdbc.api_integration.helper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CustomizedMockMvcController {
    @GetMapping("/customized_mock_mvc_test")
    fun action(): ResponseEntity<String> = ResponseEntity("OK", HttpStatus.valueOf(200))
}

@SpringBootTest
@AutoConfigureMockMvc
class CustomizedMockMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `ヘルパー-MockMvcのレスポンスのContent-Typeにはcharset=UTF-8が付いている`() {
        /**
         * given:
         */

        /**
         * when:
         */
        val actual = mockMvc.get("/customized_mock_mvc_test").andReturn().response.contentType!!

        /**
         * then:
         * - charset=UTF-8が含まれている
         */
        val expected = "charset=UTF-8"
        assertThat(actual).contains(expected)
    }
}
