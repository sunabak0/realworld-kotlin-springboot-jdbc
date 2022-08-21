package com.example.realworldkotlinspringbootjdbc.usecase.article

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UncreatedArticle
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

    sealed interface Error : MyError {
        data class InvalidArticle(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class AlreadyCreatedArticle(override val cause: MyError, val article: UncreatedArticle) :
            Error,
            MyError.MyErrorWithMyError
    }
}

@Service
class CreateArticleUseCaseImpl : CreateArticleUseCase
