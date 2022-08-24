package com.example.realworldkotlinspringbootjdbc.usecase.shared_model

import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser

data class CreatedArticleWithAuthor(
    val article: CreatedArticle,
    val author: OtherUser
)
