package com.example.realworldkotlinspringbootjdbc.domain

import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.Username

interface Profile {
    val username: Username
    val bio: Bio
    val image: Image
    val following: Boolean

    private data class ProfileWithoutValidation(
        override val username: Username,
        override val bio: Bio,
        override val image: Image,
        override val following: Boolean
    ) : Profile

    companion object {
        fun newWithoutValidation(
            username: Username,
            bio: Bio,
            image: Image,
            following: Boolean
        ): Profile =
            ProfileWithoutValidation(username, bio, image, following)
    }
}
