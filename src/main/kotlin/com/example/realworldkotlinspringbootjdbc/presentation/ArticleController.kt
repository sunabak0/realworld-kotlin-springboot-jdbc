package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.presentation.response.Article
import com.example.realworldkotlinspringbootjdbc.presentation.response.Articles
import com.example.realworldkotlinspringbootjdbc.usecase.article.ShowArticleUseCase
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

@RestController
@Tag(name = "Articles")
class ArticleController(
    val showArticle: ShowArticleUseCase,
) {
    @GetMapping("/articles")
    fun filter(@Suppress("UnusedPrivateMember") @RequestBody rawRequestBody: String?): ResponseEntity<String> {
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

    @Suppress("UnusedPrivateMember")
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
    fun feed(@Suppress("UnusedPrivateMember") @RequestBody rawRequestBody: String?): ResponseEntity<String> {
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
        val result = showArticle.execute("hoge-slug")
        when (result) {
            is Right -> {
                println(result.value)
            }
            is Left -> {}
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
    fun update(@Suppress("UnusedPrivateMember") @RequestBody rawRequestBody: String?): ResponseEntity<String> {
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
}
