package com.example.realworldkotlinspringbootjdbc.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TagController {
    @GetMapping("/api/tags")
    fun list(): ResponseEntity<String> {
        return ResponseEntity(
            ObjectMapper().writeValueAsString(
                mapOf(
                    "tags" to listOf("dragons", "training"),
                ),
            ),
            HttpStatus.valueOf(200)
        )
    }
}
