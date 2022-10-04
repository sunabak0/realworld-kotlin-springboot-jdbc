package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface ListTagUseCase {
    fun execute(): Either<Error, List<Tag>> = throw NotImplementedError()
    sealed interface Error : MyError {
        data class Unexpected(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class ListTagUseCaseImpl(
    val articleRepository: ArticleRepository
) : ListTagUseCase {
    override fun execute(): Either<ListTagUseCase.Error, List<Tag>> =
        when (val tags = articleRepository.tags()) {
            /**
             * タグ一覧 失敗
             */
            is Left -> when (val error = tags.value) {
                /**
                 * 原因: 予期せぬエラー
                 */
                is ArticleRepository.TagsError.Unexpected -> ListTagUseCase.Error.Unexpected(error).left()
            }
            /**
             * タグ一覧 成功
             */
            is Right -> tags
        }
}
