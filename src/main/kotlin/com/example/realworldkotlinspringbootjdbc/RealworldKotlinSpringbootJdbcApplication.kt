package com.example.realworldkotlinspringbootjdbc

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.DispatcherServlet


@SpringBootApplication
@OpenAPIDefinition(
    info = Info(
        title = "Conduit API",  // (1)
        description = "SpringBootでRealWorld",  // (2)
        version = "1.0.0"
    )
)
class RealworldKotlinSpringbootJdbcApplication

fun main(args: Array<String>) {
    runApplication<RealworldKotlinSpringbootJdbcApplication>(*args)
}