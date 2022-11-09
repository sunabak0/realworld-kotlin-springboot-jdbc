package com.example.realworldkotlinspringbootjdbc.usecase.shared_model

import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser

data class CommentWithAuthor(
    val comment: Comment,
    val author: OtherUser
)
