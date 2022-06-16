package com.example.realworldkotlinspringbootjdbc.domain.comment

import java.util.Date

interface Comment {
    val id: Int
    val body: String
    val createdAt: Date
    val updatedAt: Date
    val author: String
}
