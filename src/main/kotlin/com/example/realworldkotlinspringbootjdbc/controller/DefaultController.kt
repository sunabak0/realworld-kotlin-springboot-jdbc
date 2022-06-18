package com.example.realworldkotlinspringbootjdbc.controller

import com.example.realworldkotlinspringbootjdbc.controller.response.Tags
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "default")
class DefaultController {
    @GetMapping("/tags")
    fun list(): ResponseEntity<String> {
        val tags = Tags(
            listOf("dragons", "training"),
        )
        return ResponseEntity(
            ObjectMapper().writeValueAsString(tags),
            HttpStatus.valueOf(200),
        )
    }
}
