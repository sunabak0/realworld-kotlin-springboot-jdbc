package com.example.realworldkotlinspringbootjdbc.api_integration.helper

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder

/**
 * MockMvcのカスタマイズ
 *
 * カスタマイズ内容
 *
 * ResponseのContent-Typeに"charset=UTF-8"を付与
 * 理由
 * - 文字化けして日本語の比較に失敗するため
 * 参考
 * - [Spring Boot 2.2 から Content-Type: application/json に charset=UTF-8 が付かない](https://qiita.com/niwasawa/items/030f0497446918a53324)
 * - [MockMvc no longer handles UTF-8 characters with Spring Boot 2.2.0.RELEASE](https://stackoverflow.com/questions/58525387/mockmvc-no-longer-handles-utf-8-characters-with-spring-boot-2-2-0-release)
 */
@Component
class CustomizedMockMvc : MockMvcBuilderCustomizer {
    override fun customize(builder: ConfigurableMockMvcBuilder<*>?) {
        builder?.alwaysDo { result -> result.response.characterEncoding = "UTF-8" }
    }
}
