package com.example.realworldkotlinspringbootjdbc.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

//
// NullableUser
//
// 用途
// - ユーザ登録
// - プロフィール閲覧
// - ...
//
// 概要
// - nullを許容して、利用しないkeyも許容することでいろいろな箇所で使い回す
//
// 利用例
// ```
// val user = NullableUser.from("""{"user":{"email":"dummy@example.com"}}""")
// ```
//
@JsonIgnoreProperties(ignoreUnknown = true) // デシリアライズ時、利用していないkeyがあった時、それを無視する
@JsonRootName(value = "user")
data class NullableUser(
    @JsonProperty("email") val email: String?,
    @JsonProperty("password") val password: String?,
    @JsonProperty("username") val username: String?,
    @JsonProperty("bio") val bio: String?,
    @JsonProperty("image") val image: String?,
) {
    companion object {
        fun from(rawRequestBody: String?): NullableUser =
            try {
                ObjectMapper()
                    .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
                    .readValue(rawRequestBody!!)
            } catch (e: Throwable) { // どんなエラーでも拾う
                NullableUser(null, null, null, null, null)
            }
    }
}
