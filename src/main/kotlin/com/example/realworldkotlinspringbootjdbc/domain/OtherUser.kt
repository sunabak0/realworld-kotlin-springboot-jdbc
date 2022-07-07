package com.example.realworldkotlinspringbootjdbc.domain

import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username

class OtherUser private constructor(
    val userId: UserId,
    val username: Username,
    val bio: Bio,
    val image: Image,
    val following: Boolean
) {
    companion object {
        fun newWithoutValidation(
            userId: UserId,
            username: Username,
            bio: Bio,
            image: Image,
            following: Boolean
        ): OtherUser =
            OtherUser(userId, username, bio, image, following)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as OtherUser
        return this.userId == other.userId
    }

    override fun hashCode(): Int {
        return userId.value * 31
    }
}
