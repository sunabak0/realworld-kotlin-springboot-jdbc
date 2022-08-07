package com.example.realworldkotlinspringbootjdbc.usecase.article

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface CreateArticleUseCase {
    fun execute(
        currentUser: RegisteredUser,
        title: String?,
        description: String?,
        body: String?,
        tagList: List<String>?
    ): Either<Error, CreatedArticle> = TODO()

    sealed interface Error : MyError
}

@Service
class CreateArticleUseCaseImpl : CreateArticleUseCase
