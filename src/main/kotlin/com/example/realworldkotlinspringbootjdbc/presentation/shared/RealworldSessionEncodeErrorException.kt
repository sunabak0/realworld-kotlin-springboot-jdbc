package com.example.realworldkotlinspringbootjdbc.presentation.shared

import com.example.realworldkotlinspringbootjdbc.util.MySessionJwt

data class RealworldSessionEncodeErrorException(
    val error: MySessionJwt.EncodeError
) : Exception()
