package com.example.realworldkotlinspringbootjdbc.presentation.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * NullableUser
 *
 * 用途
 * - 記事投稿
 * - 作成済み記事更新
 *
 * 概要
 * - 使い回せるようにする
 *   - nullを許容する
 *   - 利用しないkeyも許容する
 *
 *  利用例
 * ```
 * val article = NullableArticle.from("""{"article":{"title":"dummy-title"}}""")
 * ```
 */
@JsonIgnoreProperties(ignoreUnknown = true) // デシリアライズ時、利用していないkeyがあった時、それを無視する
@JsonRootName(value = "article")
data class NullableArticle(
    @JsonProperty("title") val title: String?,
    @JsonProperty("description") val description: String?,
    @JsonProperty("body") val body: String?,
    @JsonProperty("tagList") val tagList: List<String>?,
) {
    companion object {
        fun from(rawRequestBody: String?): NullableArticle =
            try {
                ObjectMapper()
                    .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
                    .readValue(rawRequestBody!!)
            } catch (e: Throwable) { // どんなエラーでも拾う
                NullableArticle(
                    title = null,
                    description = null,
                    body = null,
                    tagList = null
                )
            }
    }
}
