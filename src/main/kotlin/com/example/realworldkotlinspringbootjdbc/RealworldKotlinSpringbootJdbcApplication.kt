package com.example.realworldkotlinspringbootjdbc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RealworldKotlinSpringbootJdbcApplication

fun main(args: Array<String>) {
    @Suppress("SpreadOperator")
    runApplication<RealworldKotlinSpringbootJdbcApplication>(*args)
}
