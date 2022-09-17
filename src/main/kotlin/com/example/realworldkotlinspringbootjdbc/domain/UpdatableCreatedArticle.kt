package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Option
import arrow.core.ValidatedNel
import arrow.core.invalid
import arrow.core.invalidNel
import arrow.core.validNel
import arrow.core.zip
import arrow.typeclasses.Semigroup
import com.example.realworldkotlinspringbootjdbc.domain.article.Body
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.util.MyError

/**
 * 更新可能な作成済み記事
 *
 * - オリジナルと差分が無い場合、エラーとする
 * - 項目がnullの場合は、オリジナルの方を採用する
 */
interface UpdatableCreatedArticle {
    val articleId: ArticleId
    val title: Title
    val description: Description
    val body: Body

    /**
     * 実装
     */
    private data class ValidatedUpdatableCreatedArticle(
        override val articleId: ArticleId,
        override val title: Title,
        override val description: Description,
        override val body: Body
    ) : UpdatableCreatedArticle

    companion object {
        fun new(
            originalCreatedArticle: CreatedArticle,
            title: String?,
            description: String?,
            body: String?,
        ): ValidatedNel<MyError.ValidationError, UpdatableCreatedArticle> {
            val newTitleOrOriginal: () -> ValidatedNel<Title.ValidationError, Title> = { ->
                Option.fromNullable(title).fold(
                    { originalCreatedArticle.title.validNel() }, // nullの場合は、オリジナルを採用
                    { Title.new(it) }
                )
            }
            val newDescriptionOrOriginal: () -> ValidatedNel<Description.ValidationError, Description> = { ->
                Option.fromNullable(description).fold(
                    { originalCreatedArticle.description.validNel() }, // nullの場合は、オリジナルを採用
                    { Description.new(it) }
                )
            }
            val newBodyOrOriginal: () -> ValidatedNel<Body.ValidationError, Body> = { ->
                Option.fromNullable(body).fold(
                    { originalCreatedArticle.body.validNel() }, // nullの場合は、オリジナルを採用
                    { Body.new(it) }
                )
            }

            val (validatedTitle, validatedDescription, validatedBody) =
                newTitleOrOriginal().zip(
                    Semigroup.nonEmptyList(),
                    newDescriptionOrOriginal(),
                    newBodyOrOriginal()
                ) { a, b, c -> Triple(a, b, c) }.fold(
                    { return it.invalid() }, // 1つでもバリデーションエラーがある場合: Invalid
                    { it }
                )
            return if (
                validatedTitle.value == originalCreatedArticle.title.value &&
                validatedDescription.value == originalCreatedArticle.description.value &&
                validatedBody.value == originalCreatedArticle.body.value
            ) {
                ValidationError.NothingAttributeToUpdatable.invalidNel()
            } else {

                ValidatedUpdatableCreatedArticle(
                    articleId = originalCreatedArticle.id,
                    title = validatedTitle,
                    description = validatedDescription,
                    body = validatedBody,
                ).validNel()
            }
        }
    }

    /**
     * ドメインルール
     */
    sealed interface ValidationError : MyError.ValidationError {
        /**
         * 変更が1つもないのは駄目
         */
        object NothingAttributeToUpdatable : ValidationError {
            override val key: String get() = UpdatableCreatedArticle::class.simpleName.toString()
            override val message: String get() = "更新する項目が有りません"
        }
    }
}
