package com.example.realworldkotlinspringbootjdbc.domain.profile

import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Username

interface Profile {
    val username: Username
    val bio: Bio
    val image: String
    val following: Boolean
}
