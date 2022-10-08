package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface ListTagUseCase {
    fun execute(): Either<Error, List<Tag>> = throw NotImplementedError()
    sealed interface Error : MyError
}

@Service
class ListTagUseCaseImpl(
    val articleRepository: ArticleRepository
) : ListTagUseCase {
    override fun execute(): Either<ListTagUseCase.Error, List<Tag>> = articleRepository.tags().fold(
        { TODO("成功する想定なため、この分岐に入ることはない。こういう時にUnexpected？") },
        { it.right() }
    )
}
