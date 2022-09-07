package com.example.realworldkotlinspringbootjdbc.usecase.article

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface DeleteCreatedArticleUseCase {
    fun execute(currentUser: RegisteredUser, slug: String?): Either<Error, Unit> = TODO()

    sealed interface Error : MyError {
        data class ValidationError(override val errors: List<MyError.ValidationError>) :
            Error, MyError.ValidationErrors
        data class NotFoundArticle(val slug: Slug) : Error, MyError.Basic
    }
}

@Service
class DeleteCreatedArticleUseCaseImpl : DeleteCreatedArticleUseCase
