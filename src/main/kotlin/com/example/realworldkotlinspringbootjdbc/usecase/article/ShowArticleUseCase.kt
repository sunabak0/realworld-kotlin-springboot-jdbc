package com.example.realworldkotlinspringbootjdbc.usecase.article

import arrow.core.Either
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import com.example.realworldkotlinspringbootjdbc.domain.article.Body as ArticleBody

interface ShowArticleUseCase {
    fun execute(slug: String?): Either<Error, CreatedArticle> = TODO()
    sealed interface Error : MyError {
        data class ValidationErrors(override val errors: List<MyError.ValidationError>) :
            Error,
            MyError.ValidationErrors

        data class NotFound(override val cause: MyError) : Error, MyError.MyErrorWithMyError
        data class Unexpected(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class ShowArticleUseCaseImpl : ShowArticleUseCase {
    override fun execute(slug: String?): Either<ShowArticleUseCase.Error, CreatedArticle> {
        return when (val it = Slug.new(slug)) {
            is Invalid -> ShowArticleUseCase.Error.ValidationErrors(it.value).left()
            is Valid -> {
                val a = CreatedArticle.newWithoutValidation(
                    ArticleId(1),
                    Title.newWithoutValidation("hoge-title"),
                    it.value,
                    ArticleBody.newWithoutValidation("hoge-body"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    Description.newWithoutValidation("hoge-dscription"),
                    listOf(Tag.newWithoutValidation("dragons"), Tag.newWithoutValidation("training")),
                    UserId(1),
                    true,
                    1
                )
                a.right()
            }
        }
    }
}
