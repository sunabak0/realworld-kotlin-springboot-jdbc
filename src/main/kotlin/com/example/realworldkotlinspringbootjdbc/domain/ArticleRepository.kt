package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface ArticleRepository {
    /**
     * タグ一覧
     */
    fun tags(): Either<TagsError, List<Tag>> = TODO("テストでDIする時、余計なoverrideを記述不要にするため")
    sealed interface TagsError : MyError {
        data class Unexpected(override val cause: Throwable) : TagsError, MyError.MyErrorWithThrowable
    }
}
