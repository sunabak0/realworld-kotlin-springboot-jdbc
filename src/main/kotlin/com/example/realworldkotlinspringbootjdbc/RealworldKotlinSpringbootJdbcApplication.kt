package com.example.realworldkotlinspringbootjdbc

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@OpenAPIDefinition(
    info = Info(
        title = "Conduit API",
        description = "SpringBootでRealWorld",
        version = "1.0.0"
    )
)
class RealworldKotlinSpringbootJdbcApplication

fun main(args: Array<String>) {
    runApplication<RealworldKotlinSpringbootJdbcApplication>(*args)
}
