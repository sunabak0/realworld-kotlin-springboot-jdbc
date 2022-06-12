package com.example.realworldkotlinspringbootjdbc.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class ArticleController {
    @GetMapping("/api/articles")
    fun filter(): ResponseEntity<String> {
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
        ), HttpStatus.valueOf(200))
    }

    @PostMapping("/api/articles")
    fun create(): ResponseEntity<String> {
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
            ), HttpStatus.valueOf(200))
    }

    @GetMapping("/api/articles/feed")
    fun feed(): ResponseEntity<String> {
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
            ), HttpStatus.valueOf(200))
    }

    @GetMapping("/api/articles/{slug}")
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
            ), HttpStatus.valueOf(200))
    }

    @PutMapping("/api/articles/{slug}")
    fun update(): ResponseEntity<String> {
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
            ), HttpStatus.valueOf(200))
    }

    @DeleteMapping("/api/articles/{slug}")
    fun delete(): ResponseEntity<String> {
        return ResponseEntity("", HttpStatus.valueOf(200))
    }

    @PostMapping("/api/articles/{slug}/favorite")
    fun favorite(): ResponseEntity<String> {
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
                        "favorited" to true,
                        "favoritesCount" to 1,
                    ),
                )
            ), HttpStatus.valueOf(200))
    }

    @DeleteMapping("/api/articles/{slug}/favorite")
    fun unfavorite(): ResponseEntity<String> {
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
            ), HttpStatus.valueOf(200))
    }
}