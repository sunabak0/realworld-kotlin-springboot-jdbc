package com.example.realworldkotlinspringbootjdbc.controller.response

import com.fasterxml.jackson.annotation.JsonProperty

data class Tags(
    @JsonProperty("tags") val tags: List<String>,
)
