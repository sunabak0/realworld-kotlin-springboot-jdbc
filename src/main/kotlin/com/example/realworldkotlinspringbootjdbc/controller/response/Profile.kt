package com.example.realworldkotlinspringbootjdbc.controller.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName

@JsonRootName(value = "profile")
data class Profile(
    @JsonProperty("username") val username: String,
    @JsonProperty("bio") val bio: String,
    @JsonProperty("image") val image: String,
    @JsonProperty("following") val following: Boolean,
)
