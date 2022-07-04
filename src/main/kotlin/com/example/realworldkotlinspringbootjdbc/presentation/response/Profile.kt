package com.example.realworldkotlinspringbootjdbc.presentation.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

@JsonRootName(value = "profile")
data class Profile(
    @JsonProperty("username") val username: String,
    @JsonProperty("bio") val bio: String,
    @JsonProperty("image") val image: String,
    @JsonProperty("following") val following: Boolean,
) {
    /**
     * Factory メソッド
     */
    companion object {
        fun from(otherUser: com.example.realworldkotlinspringbootjdbc.domain.OtherUser): Profile = Profile(
            otherUser.username.value,
            otherUser.bio.value,
            otherUser.image.value,
            otherUser.following
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
