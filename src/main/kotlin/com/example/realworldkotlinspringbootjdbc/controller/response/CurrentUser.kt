package com.example.realworldkotlinspringbootjdbc.controller.response

import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

@JsonRootName(value = "user")
data class CurrentUser(
    @JsonProperty("email") val email: String,
    @JsonProperty("username") val username: String,
    @JsonProperty("bio") val bio: String,
    @JsonProperty("image") val image: String,
    @JsonProperty("token") val token: String,
) {
    companion object {
        fun from(user: RegisteredUser, token: String): CurrentUser =
            CurrentUser(
                user.email.value,
                user.username.value,
                user.bio.value,
                user.image.value,
                token,
            )
    }

    fun serializeWithRootName(): String =
        ObjectMapper()
            .enable(SerializationFeature.WRAP_ROOT_VALUE)
            .writeValueAsString(this)
}
