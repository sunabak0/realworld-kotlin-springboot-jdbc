package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.util.Date

interface ListCommentsUseCase {
    fun execute(slug: String?): Either<Error, List<Comment>> = Error.NotImplemented.left()
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
class ShowCommentsUseCaseImpl() : ListCommentsUseCase {
    override fun execute(slug: String?): Either<ListCommentsUseCase.Error, List<Comment>> {
        return when (val it = Slug.new(slug)) {
            is Invalid -> ListCommentsUseCase.Error.ValidationErrors(it.value).left()
            is Valid -> {
                val a = object : Comment {
                    override val id: Int
                        get() = 1
                    override val body: String
                        get() = "hoge-body-1"
                    override val createdAt: Date
                        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
                    override val updatedAt: Date
                        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
                    override val author: String
                        get() = "hoge-author-1"
                }
                val b = object : Comment {
                    override val id: Int
                        get() = 2
                    override val body: String
                        get() = "hoge-body-2"
                    override val createdAt: Date
                        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00")
                    override val updatedAt: Date
                        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00")
                    override val author: String
                        get() = "hoge-author-2"
                }
                listOf(a, b).right()
            }
        }
    }
}
