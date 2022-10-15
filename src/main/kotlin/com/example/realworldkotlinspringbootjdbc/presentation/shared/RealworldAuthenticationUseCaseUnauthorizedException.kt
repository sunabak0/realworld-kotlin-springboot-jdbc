package com.example.realworldkotlinspringbootjdbc.presentation.shared

import com.example.realworldkotlinspringbootjdbc.usecase.shared.RealworldAuthenticationUseCase

/**
 * 認証失敗エラーの例外ラッパー
 *
 * @property error 認証失敗時の原因
 */
data class RealworldAuthenticationUseCaseUnauthorizedException(
    val error: RealworldAuthenticationUseCase.Unauthorized
) : Exception()
