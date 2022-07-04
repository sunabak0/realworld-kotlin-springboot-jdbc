package com.example.realworldkotlinspringbootjdbc.domain

import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username

interface OtherUser {
    val userId: UserId
    val username: Username
    val bio: Bio
    val image: Image
    val following: Boolean

    private data class OtherUserWithoutValidation(
        override val userId: UserId,
        override val username: Username,
        override val bio: Bio,
        override val image: Image,
        override val following: Boolean
    ) : OtherUser

    companion object {
        fun newWithoutValidation(
            userId: UserId,
            username: Username,
            bio: Bio,
            image: Image,
            following: Boolean
        ): OtherUser =
            OtherUserWithoutValidation(userId, username, bio, image, following)
    }
}
