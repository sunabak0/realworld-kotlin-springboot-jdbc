package com.example.realworldkotlinspringbootjdbc.usecase.favorite

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface UnfavoriteUseCase {
    fun execute(slug: String?, currentUser: RegisteredUser): Either<Error, Unit> = TODO()
    sealed interface Error : MyError {
        data class InvalidSlug(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class ArticleNotFoundBySlug(override val cause: MyError, val slug: Slug) :
            Error,
            MyError.MyErrorWithMyError

        data class Unexpected(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}
