package com.example.realworldkotlinspringbootjdbc.domain

import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import java.util.Date
import com.example.realworldkotlinspringbootjdbc.domain.article.Body as ArticleBody

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CreatedArticle
        return this.id == other.id
    }

    override fun hashCode(): Int {
        return id.value * 31
    }

    /**
     * 対象のTagを持っているか
     *
     * @param tag
     * @return 持っている: true or 持っていない: false
     */
    fun hasTag(tag: Tag): Boolean = tagList.contains(tag)
}
