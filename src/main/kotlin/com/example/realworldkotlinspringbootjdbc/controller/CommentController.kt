package com.example.realworldkotlinspringbootjdbc.controller

import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.service.CommentService
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.text.SimpleDateFormat
import java.util.Date

@RestController
@Tag(name = "Comments")
class CommentController(val commentService: CommentService) {
    @GetMapping("/articles/{slug}/comments")
    fun list(): ResponseEntity<String> {
        val result = commentService.list("hoge-slug")
        when (result) {
            is Right -> {
                println(result.value)
            }
            is Left -> {}
        }
        val comment1 = Comment(
            1,
            "hoge-body-1",
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
            "hoge-author-1"
        )
        val comment2 = Comment(
            2,
            "hoge-body-2",
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
            "hoge-author-2"
        )
        return ResponseEntity(
            ObjectMapper().writeValueAsString(
                mapOf(
                    "comments" to listOf(
                        comment1,
                        comment2,
                    ),
                )
            ),
            HttpStatus.valueOf(200)
        )
    }

    @PostMapping("/articles/{slug}/comments")
    fun create(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        return ResponseEntity(
            ObjectMapper().writeValueAsString(
                mapOf(
                    "comment" to mapOf(
                        "id" to 1,
                        "body" to "hoge-body",
                        "createdAt" to "2022-01-01T00:00:00.0+09:00",
                        "updatedAt" to "2022-01-01T00:00:00.0+09:00",
                        "author" to "hoge-author",
                    ),
                )
            ),
            HttpStatus.valueOf(200)
        )
    }

    @DeleteMapping("/articles/{slug}/comments/{id}")
    fun delete(): ResponseEntity<String> {
        return ResponseEntity("", HttpStatus.valueOf(200))
    }

    data class Comment(
        @JsonProperty("id") val id: Int,
        @JsonProperty("body") val body: String,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        @JsonProperty("createdAt")
        val createdAt: Date,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        @JsonProperty("updatedAt")
        val updatedAt: Date,
        @JsonProperty("author") val author: String,
    )
}
