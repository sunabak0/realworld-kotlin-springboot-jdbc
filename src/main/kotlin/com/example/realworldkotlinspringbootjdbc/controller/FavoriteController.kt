package com.example.realworldkotlinspringbootjdbc.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Favorites")
class FavoriteController {
    @PostMapping("/articles/{slug}/favorite")
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
            ),
            HttpStatus.valueOf(200)
        )
    }

    @DeleteMapping("/articles/{slug}/favorite")
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
            ),
            HttpStatus.valueOf(200)
        )
    }
}
