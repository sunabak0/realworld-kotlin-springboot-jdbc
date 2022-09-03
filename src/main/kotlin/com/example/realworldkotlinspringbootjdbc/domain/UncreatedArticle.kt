package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Invalid
import arrow.core.NonEmptyList
import arrow.core.Option
import arrow.core.Valid
import arrow.core.ValidatedNel
import arrow.core.invalid
import arrow.core.valid
import arrow.core.zip
import arrow.typeclasses.Semigroup
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.util.MyError.ValidationError
import com.example.realworldkotlinspringbootjdbc.domain.article.Body as ArticleBody

interface UncreatedArticle {
    val title: Title
    val slug: Slug
    val description: Description
    val body: ArticleBody
    val tagList: List<Tag>
    val authorId: UserId

    /**
     * 実装
     */
    private data class ValidatedUncreatedArticle(
        override val title: Title,
        override val slug: Slug,
        override val description: Description,
        override val body: ArticleBody,
        override val tagList: List<Tag>,
        override val authorId: UserId
    ) : UncreatedArticle

    /**
     * Factory メソッド
     */
    companion object {
        fun new(
            title: String?,
            description: String?,
            body: String?,
            tagList: List<String>?,
            authorId: UserId,
        ): ValidatedNel<ValidationError, UncreatedArticle> {
            val convertToValidTagList: (List<String>?) -> ValidatedNel<ValidationError, List<Tag>> = { input ->
                Option.fromNullable(input).fold(
                    { listOf<Tag>().valid() },
                    { tagList ->
                        val errors = mutableListOf<ValidationError>()
                        val validatedTagList = mutableListOf<Tag>()
                        tagList.forEach {
                            when (val tag = Tag.new(it)) {
                                is Invalid -> errors.addAll(tag.value)
                                is Valid -> validatedTagList.add(tag.value)
                            }
                        }
                        when (errors.isEmpty()) {
                            false -> NonEmptyList.fromList(errors).orNull()!!.invalid()
                            true -> validatedTagList.valid()
                        }
                    }
                )
            }
            return Title.new(title).zip(
                Semigroup.nonEmptyList(),
                Description.new(description),
                ArticleBody.new(body),
                convertToValidTagList(tagList)
            ) { a, b, c, d -> ValidatedUncreatedArticle(a, Slug.new(), b, c, d, authorId) }
        }
    }
}
