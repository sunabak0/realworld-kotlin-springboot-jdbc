package com.example.realworldkotlinspringbootjdbc.domain

import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import java.util.Date

interface Article {
    val title: Title
    val slug: Slug
    val body: String
    val createdAt: Date
    val updatedAt: Date
    val description: String
    val tagList: List<Tag>
    val author: Profile
    val favorited: Boolean
    val favoritesCount: Int
}
