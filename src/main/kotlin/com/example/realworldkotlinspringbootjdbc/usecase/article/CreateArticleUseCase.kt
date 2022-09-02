package com.example.realworldkotlinspringbootjdbc.usecase.article

import arrow.core.Either
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
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
class CreateArticleUseCaseImpl : CreateArticleUseCase {
    override fun execute(
        currentUser: RegisteredUser,
        title: String?,
        description: String?,
        body: String?,
        tagList: List<String>?
    ): Either<CreateArticleUseCase.Error, CreatedArticle> {
        return when (
            val uncreatedArticle =
                UncreatedArticle.new(title, description, body, tagList, currentUser.userId)
        ) {
            is Invalid -> CreateArticleUseCase.Error.InvalidArticle(uncreatedArticle.value).left()
            is Valid -> TODO()
        }
    }
}
