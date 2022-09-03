package com.example.realworldkotlinspringbootjdbc.api_integration

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
@Tag("ApiIntegration")
class HealthCheckTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `正常系-ヘルスチェックに成功する`() {
        /**
         * given:
         */

        /**
         * when:
         */
        val result = mockMvc.get("/actuator/health")

        /**
         * then:
         */
        result
            .andExpect { status { isOk() } }
            .andExpect { content { json("""{"status":"UP"}""") } }
    }
}
