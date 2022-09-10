package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.invalid
import arrow.core.valid
import com.example.realworldkotlinspringbootjdbc.domain.article.Body
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.IntRange
import net.jqwik.api.constraints.Size
import net.jqwik.api.constraints.UniqueElements
import org.assertj.core.api.Assertions.assertThat
import java.util.Date

class CreatedArticleAuthorValidationDomainServiceTest {
    @Property
    fun `正常系-作成済み記事の著者である場合、有効である旨が戻り値`(
        @ForAll @IntRange(min = 1) userId: Int
    ) {
        /**
         * given:
         * - UserIdが同じ 作成済み記事 と 登録済みユーザー
         */
        val createdArticle = CreatedArticle.newWithoutValidation(
            id = ArticleId(1),
            title = Title.newWithoutValidation("fake-title"),
            slug = Slug.newWithoutValidation("fake-slug"),
            body = Body.newWithoutValidation("fake-body"),
            createdAt = Date(),
            updatedAt = Date(),
            description = Description.newWithoutValidation("fake-description"),
            tagList = emptyList(),
            authorId = UserId(userId),
            favorited = false,
            favoritesCount = 0
        )
        val registeredUser = RegisteredUser.newWithoutValidation(
            userId = UserId(userId),
            email = Email.newWithoutValidation("fake-email@example.com"),
            username = Username.newWithoutValidation("fake-username"),
            bio = Bio.newWithoutValidation("fake-bio"),
            image = Image.newWithoutValidation("fake-image"),
        )

        /**
         * when:
         */
        val actual = CreatedArticleAuthorValidationDomainService.validate(
            article = createdArticle,
            user = registeredUser
        )

        /**
         * then:
         */
        val expected = Unit.valid()
        assertThat(actual).isEqualTo(expected)
    }

    @Property
    fun `準正常系-作成済み記事の著者ではない場合、有効ではない旨が戻り値`(
        @ForAll @UniqueElements @Size(2) uniqueIdIntList: List<@IntRange(min = 1) Int>
    ) {
        /**
         * given:
         * - UserIdが異なる 作成済み記事 と 登録済みユーザー
         */
        val userId = uniqueIdIntList[0]
        val diffUserId = uniqueIdIntList[1]
        val createdArticle = CreatedArticle.newWithoutValidation(
            id = ArticleId(1),
            title = Title.newWithoutValidation("fake-title"),
            slug = Slug.newWithoutValidation("fake-slug"),
            body = Body.newWithoutValidation("fake-body"),
            createdAt = Date(),
            updatedAt = Date(),
            description = Description.newWithoutValidation("fake-description"),
            tagList = emptyList(),
            authorId = UserId(userId),
            favorited = false,
            favoritesCount = 0
        )
        val registeredUser = RegisteredUser.newWithoutValidation(
            userId = UserId(diffUserId),
            email = Email.newWithoutValidation("fake-email@example.com"),
            username = Username.newWithoutValidation("fake-username"),
            bio = Bio.newWithoutValidation("fake-bio"),
            image = Image.newWithoutValidation("fake-image"),
        )

        /**
         * when:
         */
        val actual = CreatedArticleAuthorValidationDomainService.validate(
            article = createdArticle,
            user = registeredUser
        )

        /**
         * then:
         */
        val expected = CreatedArticleAuthorValidationDomainService.Error.NotMatchedUserId(
            article = createdArticle,
            user = registeredUser
        ).invalid()
        assertThat(actual).isEqualTo(expected)
    }
}
