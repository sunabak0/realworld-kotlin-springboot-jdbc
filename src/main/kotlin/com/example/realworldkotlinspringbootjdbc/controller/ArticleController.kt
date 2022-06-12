package com.example.realworldkotlinspringbootjdbc.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Articles")
class ArticleController {
    @GetMapping("/articles")
    fun filter(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        return ResponseEntity(
            ObjectMapper().writeValueAsString(
                mapOf(
                    "articlesCount" to 1,
                    "articles" to listOf(
                        mapOf(
                            "title" to "hoge-title",
                            "slug" to "hoge-slug",
                            "body" to "hoge-body",
                            "createdAt" to "2022-01-01T00:00:00.0+09:00",
                            "updatedAt" to "2022-01-01T00:00:00.0+09:00",
                            "description" to "hoge-description",
                            "tagList" to listOf("dragons", "training"),
                            "author" to "hoge-author",
                            "favorited" to true,
                            "favoritesCount" to 1,
                        )
                    ),
                )
            ),
            HttpStatus.valueOf(200)
        )
    }

    @PostMapping("/articles")
    fun create(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        return ResponseEntity(
            ObjectMapper().writeValueAsString(
                mapOf(
                    "article" to mapOf(
                        "title" to "hoge-title",
                        "slug" to "hoge-slug",
                        "body" to "hoge-body",
                        "createdAt" to "2022-01-01T00:00:00.0+09:00",
                        "updatedAt" to "2022-01-01T00:00:00.0+09:00",
                        "description" to "hoge-description",
                        "tagList" to listOf("dragons", "training"),
                        "author" to "hoge-author",
                        "favorited" to false,
                        "favoritesCount" to 0,
                    ),
                )
            ),
            HttpStatus.valueOf(200)
        )
    }

    @GetMapping("/articles/feed")
    fun feed(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        return ResponseEntity(
            ObjectMapper().writeValueAsString(
                mapOf(
                    "articlesCount" to 1,
                    "articles" to listOf(
                        mapOf(
                            "title" to "hoge-title",
                            "slug" to "hoge-slug",
                            "body" to "hoge-body",
                            "createdAt" to "2022-01-01T00:00:00.0+09:00",
                            "updatedAt" to "2022-01-01T00:00:00.0+09:00",
                            "description" to "hoge-description",
                            "tagList" to listOf("dragons", "training"),
                            "author" to "hoge-author",
                            "favorited" to true,
                            "favoritesCount" to 1,
                        )
                    ),
                )
            ),
            HttpStatus.valueOf(200)
        )
    }

    @GetMapping("/articles/{slug}")
    fun show(): ResponseEntity<String> {
        return ResponseEntity(
            ObjectMapper().writeValueAsString(
                mapOf(
                    "article" to mapOf(
                        "title" to "hoge-title",
                        "slug" to "hoge-slug",
                        "body" to "hoge-body",
                        "createdAt" to "2022-01-01T00:00:00.0+09:00",
                        "updatedAt" to "2022-01-01T00:00:00.0+09:00",
                        "description" to "hoge-description",
                        "tagList" to listOf("dragons", "training"),
                        "author" to "hoge-author",
                        "favorited" to false,
                        "favoritesCount" to 0,
                    ),
                )
            ),
            HttpStatus.valueOf(200)
        )
    }

    @PutMapping("/articles/{slug}")
    fun update(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        return ResponseEntity(
            ObjectMapper().writeValueAsString(
                mapOf(
                    "article" to mapOf(
                        "title" to "hoge-title",
                        "slug" to "hoge-slug",
                        "body" to "hoge-body",
                        "createdAt" to "2022-01-01T00:00:00.0+09:00",
                        "updatedAt" to "2022-01-01T00:00:00.0+09:00",
                        "description" to "hoge-description",
                        "tagList" to listOf("dragons", "training"),
                        "author" to "hoge-author",
                        "favorited" to false,
                        "favoritesCount" to 0,
                    ),
                )
            ),
            HttpStatus.valueOf(200)
        )
    }

    @DeleteMapping("/articles/{slug}")
    fun delete(): ResponseEntity<String> {
        return ResponseEntity("", HttpStatus.valueOf(200))
    }
}
