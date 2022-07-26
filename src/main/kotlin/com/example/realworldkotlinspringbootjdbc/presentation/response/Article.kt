package com.example.realworldkotlinspringbootjdbc.presentation.response

import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import java.util.Date

@JsonRootName(value = "article")
data class Article(
    @JsonProperty("title") val title: String,
    @JsonProperty("slug") val slug: String,
    @JsonProperty("body") val body: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @JsonProperty("createdAt")
    val createdAt: Date,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @JsonProperty("updatedAt")
    val updatedAt: Date,
    @JsonProperty("description") val description: String,
    @JsonProperty("tagList") val tagList: List<String>,
    // TODO: authorId を author に変更
    @JsonProperty("authorId") val authorId: Int,
    @JsonProperty("favorited") val favorited: Boolean,
    @JsonProperty("favoritesCount") val favoritesCount: Int,
) {
    /**
     * Factory メソッド
     */
    companion object {
        fun from(createdArticle: CreatedArticle): Article =
            Article(
                title = createdArticle.title.value,
                slug = createdArticle.slug.value,
                body = createdArticle.body.value,
                createdAt = createdArticle.createdAt,
                updatedAt = createdArticle.updatedAt,
                description = createdArticle.description.value,
                tagList = createdArticle.tagList.map { tag -> tag.value },
                // TODO: authorId を author に変更
                authorId = createdArticle.authorId.value,
                favorited = createdArticle.favorited,
                favoritesCount = createdArticle.favoritesCount
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

data class Articles(
    @JsonProperty("articlesCount") val articlesCount: Int,
    @JsonProperty("articles") val articles: List<Article>,
)
