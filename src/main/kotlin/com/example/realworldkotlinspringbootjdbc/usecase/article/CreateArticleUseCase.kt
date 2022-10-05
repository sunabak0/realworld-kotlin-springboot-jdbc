package com.example.realworldkotlinspringbootjdbc.usecase.article

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Validated.Invalid
import arrow.core.Validated.Valid
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UncreatedArticle
import com.example.realworldkotlinspringbootjdbc.usecase.shared_model.CreatedArticleWithAuthor
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface CreateArticleUseCase {
    fun execute(
        currentUser: RegisteredUser,
        title: String?,
        description: String?,
        body: String?,
        tagList: List<String>?
    ): Either<Error, CreatedArticleWithAuthor> = throw NotImplementedError()

    sealed interface Error : MyError {
        data class InvalidArticle(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
    }
}

@Service
class CreateArticleUseCaseImpl(
    val articleRepository: ArticleRepository,
) : CreateArticleUseCase {
    override fun execute(
        currentUser: RegisteredUser,
        title: String?,
        description: String?,
        body: String?,
        tagList: List<String>?
    ): Either<CreateArticleUseCase.Error, CreatedArticleWithAuthor> =
        when (
            val uncreatedArticle =
                UncreatedArticle.new(title, description, body, tagList, currentUser.userId)
        ) {
            /**
             * バリデーションエラー
             */
            is Invalid -> CreateArticleUseCase.Error.InvalidArticle(uncreatedArticle.value).left()
            /**
             * 未作成記事 -> 保存 -> 作成済み記事with著者情報
             */
            is Valid -> when (val createdArticle = articleRepository.create(uncreatedArticle.value)) {
                is Left -> { TODO("成功する想定なため、この分岐に入ることはない。こういう時にUnexpected？") }
                is Right -> CreatedArticleWithAuthor(
                    article = createdArticle.value,
                    author = OtherUser.newWithoutValidation(
                        userId = currentUser.userId,
                        username = currentUser.username,
                        bio = currentUser.bio,
                        image = currentUser.image,
                        following = false,
                    )
                ).right()
            }
        }
}
