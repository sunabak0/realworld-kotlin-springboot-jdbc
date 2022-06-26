package com.example.realworldkotlinspringbootjdbc.presentation.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * NullableComment
 *
 * 用途
 * - コメント登録
 *
 * 利用例
 * ```
 * val comment = NullableComment.from("""{"comment":{"body":"dummy-body"}}""")
 * ```
 */
@JsonIgnoreProperties(ignoreUnknown = true) // デシリアライズ時、利用していないkeyがあった時、それを無視する
@JsonRootName(value = "comment")
data class NullableComment(
    @JsonProperty("body") val body: String?
) {
    companion object {
        fun from(rawRequestBody: String?): NullableComment =
            try {
                ObjectMapper()
                    .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
                    .readValue(rawRequestBody!!)
            } catch (e: Throwable) {
                NullableComment(null)
            }
    }
}
