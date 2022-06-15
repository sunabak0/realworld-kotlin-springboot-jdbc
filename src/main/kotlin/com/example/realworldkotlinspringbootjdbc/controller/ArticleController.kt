package com.example.realworldkotlinspringbootjdbc.controller

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.service.ArticleService
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.text.SimpleDateFormat
import java.util.Date

@RestController
@Tag(name = "Articles")
class ArticleController(
    val articleService: ArticleService,
) {
    @GetMapping("/articles")
    fun filter(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        val articles = Articles(
            1,
            listOf(
                Article(
                    "hoge-title",
                    "hoge-slug",
                    "hoge-body",
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    "hoge-description",
                    listOf("dragons", "training"),
                    "hoge-author",
                    true,
                    1,
                )
            )
        )
        return ResponseEntity(
            ObjectMapper().writeValueAsString(articles),
            HttpStatus.valueOf(200)
        )
    }

    @PostMapping("/articles")
    fun create(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        val article = Article(
            "hoge-title",
            "hoge-slug",
            "hoge-body",
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            "hoge-description",
            listOf("dragons", "training"),
            "hoge-author",
            true,
            1,
        )
        return ResponseEntity(
            ObjectMapper().enable(SerializationFeature.WRAP_ROOT_VALUE).writeValueAsString(article),
            HttpStatus.valueOf(200),
        )
    }

    @GetMapping("/articles/feed")
    fun feed(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        val articles = Articles(
            1,
            listOf(
                Article(
                    "hoge-title",
                    "hoge-slug",
                    "hoge-body",
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    "hoge-description",
                    listOf("dragons", "training"),
                    "hoge-author",
                    true,
                    1,
                )
            )
        )
        return ResponseEntity(
            ObjectMapper().writeValueAsString(articles),
            HttpStatus.valueOf(200)
        )
    }

    @GetMapping("/articles/{slug}")
    fun show(): ResponseEntity<String> {
        val result = articleService.show("hoge-slug")
        when (result) {
            is Either.Right -> {
                println(result.value)
            }
            is Either.Left -> {}
        }
        val article = Article(
            "hoge-title",
            "hoge-slug",
            "hoge-body",
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            "hoge-description",
            listOf("dragons", "training"),
            "hoge-author",
            true,
            1,
        )
        return ResponseEntity(
            ObjectMapper().enable(SerializationFeature.WRAP_ROOT_VALUE).writeValueAsString(article),
            HttpStatus.valueOf(200),
        )
    }

    @PutMapping("/articles/{slug}")
    fun update(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        val article = Article(
            "hoge-title",
            "hoge-slug",
            "hoge-body",
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            "hoge-description",
            listOf("dragons", "training"),
            "hoge-author",
            true,
            1,
        )
        return ResponseEntity(
            ObjectMapper().enable(SerializationFeature.WRAP_ROOT_VALUE).writeValueAsString(article),
            HttpStatus.valueOf(200),
        )
    }

    @DeleteMapping("/articles/{slug}")
    fun delete(): ResponseEntity<String> {
        return ResponseEntity("", HttpStatus.valueOf(200))
    }

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
        @JsonProperty("author") val author: String,
        @JsonProperty("favorited") val favorited: Boolean,
        @JsonProperty("favoritesCount") val favoritesCount: Int,
    )

    data class Articles(
        @JsonProperty("articlesCount") val articlesCount: Int,
        @JsonProperty("articles") val articles: List<Article>,
    )
}
