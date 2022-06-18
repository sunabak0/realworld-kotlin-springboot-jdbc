package com.example.realworldkotlinspringbootjdbc.service

import arrow.core.Either
import arrow.core.Invalid
import arrow.core.Valid
import com.example.realworldkotlinspringbootjdbc.domain.article.Article
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.util.Date

interface ArticleService {
    fun show(slug: String?): Either<ShowError, Article> = Either.Left(ShowError.NotImplemented)
    sealed interface ShowError : MyError {
        data class ValidationErrors(override val errors: List<MyError.ValidationError>) :
            ShowError,
            MyError.ValidationErrors

        data class FailedShow(override val cause: MyError) : ShowError, MyError.MyErrorWithMyError
        data class NotFound(override val cause: MyError) : ShowError, MyError.MyErrorWithMyError
        object NotImplemented : ShowError
    }
}

@Service
class ArticleServiceImpl() : ArticleService {
    override fun show(slug: String?): Either<ArticleService.ShowError, Article> {
        return when (val it = Slug.new(slug)) {
            is Invalid -> Either.Left(ArticleService.ShowError.ValidationErrors(it.value))
            is Valid -> {
                val a = object : Article {
                    override val title: String
                        get() = "hoge-title"
                    override val slug: Slug
                        get() = it.value
                    override val body: String
                        get() = "hoge-body"
                    override val createdAt: Date
                        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
                    override val updatedAt: Date
                        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
                    override val description: String
                        get() = "hoge-description"
                    override val tagList: List<String>
                        get() = listOf("dragons", "training")
                    override val author: String
                        get() = "hoge-author"
                    override val favorited: Boolean
                        get() = true
                    override val favoritesCount: Int
                        get() = 1
                }
                Either.Right(a)
            }
        }
    }
}
