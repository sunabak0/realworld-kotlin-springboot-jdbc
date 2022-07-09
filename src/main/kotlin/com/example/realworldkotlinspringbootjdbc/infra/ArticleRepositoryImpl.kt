package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import org.springframework.stereotype.Repository

@Repository
class ArticleRepositoryImpl : ArticleRepository {
    override fun tags(): Either<ArticleRepository.TagsError, List<Tag>> {
        /**
         * TODO: DBから引っ張ってくる
         */
        return listOf(
            Tag.newWithoutValidation("dragons"),
            Tag.newWithoutValidation("training"),
        ).right()
    }
}
