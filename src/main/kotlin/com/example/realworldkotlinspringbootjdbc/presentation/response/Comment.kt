package com.example.realworldkotlinspringbootjdbc.presentation.response

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import java.util.Date

data class Comment(
    @JsonProperty("id") val id: Int,
    @JsonProperty("body") val body: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @JsonProperty("createdAt")
    val createdAt: Date,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @JsonProperty("updatedAt")
    val updatedAt: Date,
    @JsonProperty("author") val author: String,
) {
    /**
     * Factory メソッド
     */
    companion object {
        fun from(comment: com.example.realworldkotlinspringbootjdbc.domain.Comment): Comment =
            Comment(
                comment.id.value,
                comment.body.value,
                comment.createdAt,
                comment.updatedAt,
                comment.author.username.value,
            )
    }

    /**
     * JSON へシリアライズ
     */
    fun serializeWithRootName(): String =
        ObjectMapper()
            .enable(SerializationFeature.WRAP_ROOT_VALUE)
            .writeValueAsString(this)
}
