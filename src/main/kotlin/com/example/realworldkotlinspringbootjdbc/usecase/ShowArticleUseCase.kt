package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.util.Date

interface ShowArticleUseCase {
    fun execute(slug: String?): Either<Error, CreatedArticle> = Error.NotImplemented.left()
    sealed interface Error : MyError {
        data class ValidationErrors(override val errors: List<MyError.ValidationError>) :
            Error,
            MyError.ValidationErrors

        data class FailedShow(override val cause: MyError) : Error, MyError.MyErrorWithMyError
        data class NotFound(override val cause: MyError) : Error, MyError.MyErrorWithMyError
        object NotImplemented : Error
    }
}

@Service
class ShowArticleUseCaseImpl() : ShowArticleUseCase {
    override fun execute(slug: String?): Either<ShowArticleUseCase.Error, CreatedArticle> {
        return when (val it = Slug.new(slug)) {
            is Invalid -> ShowArticleUseCase.Error.ValidationErrors(it.value).left()
            is Valid -> {
                val a = object : CreatedArticle {
                    override val title: Title
                        get() = Title.newWithoutValidation("hoge-title")
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
                    override val tagList: List<Tag>
                        get() = listOf(Tag.newWithoutValidation("dragons"), Tag.newWithoutValidation("training"))
                    override val author: OtherUser
                        get() = OtherUser.newWithoutValidation(
                            UserId(1),
                            Username.newWithoutValidation("hoge-username"),
                            Bio.newWithoutValidation("hoge-bio"),
                            Image.newWithoutValidation("hoge-image"),
                            true,
                        )
                    override val favorited: Boolean
                        get() = true
                    override val favoritesCount: Int
                        get() = 1
                }
                a.right()
            }
        }
    }
}
