package com.example.realworldkotlinspringbootjdbc.presentation

import com.example.realworldkotlinspringbootjdbc.presentation.response.Article
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.text.SimpleDateFormat

@RestController
@Tag(name = "Favorites")
class FavoriteController {
    @PostMapping("/articles/{slug}/favorite")
    fun favorite(): ResponseEntity<String> {
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

    @DeleteMapping("/articles/{slug}/favorite")
    fun unfavorite(): ResponseEntity<String> {
        val article = Article(
            "hoge-title",
            "hoge-slug",
            "hoge-body",
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            "hoge-description",
            listOf("dragons", "training"),
            "hoge-author",
            false,
            0,
        )
        return ResponseEntity(
            ObjectMapper().enable(SerializationFeature.WRAP_ROOT_VALUE).writeValueAsString(article),
            HttpStatus.valueOf(200),
        )
    }
}
