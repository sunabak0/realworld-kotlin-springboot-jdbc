package com.example.realworldkotlinspringbootjdbc.domain

import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Body as ArticleBody
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import java.util.Date

class CreatedArticle private constructor(
    val id: ArticleId,
    val title: Title,
    val slug: Slug,
    val body: ArticleBody,
    val createdAt: Date,
    val updatedAt: Date,
    val description: Description,
    val tagList: List<Tag>,
    val authorId: UserId,
    val favorited: Boolean,
    val favoritesCount: Int,
) {
    companion object {
        fun newWithoutValidation(
            id: ArticleId,
            title: Title,
            slug: Slug,
            body: ArticleBody,
            createdAt: Date,
            updatedAt: Date,
            description: Description,
            tagList: List<Tag>,
            authorId: UserId,
            favorited: Boolean,
            favoritesCount: Int,
        ): CreatedArticle = CreatedArticle(
            id,
            title,
            slug,
            body,
            createdAt,
            updatedAt,
            description,
            tagList,
            authorId,
            favorited,
            favoritesCount
        )
    }
}
