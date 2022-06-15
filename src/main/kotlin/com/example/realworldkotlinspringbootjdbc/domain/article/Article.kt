package com.example.realworldkotlinspringbootjdbc.domain.article

import java.util.Date

interface Article {
    val title: String
    val slug: Slug
    val body: String
    val createdAt: Date
    val updatedAt: Date
    val description: String
    val tagList: List<String>
    val author: String
    val favorited: Boolean
    val favoritesCount: Int
}
