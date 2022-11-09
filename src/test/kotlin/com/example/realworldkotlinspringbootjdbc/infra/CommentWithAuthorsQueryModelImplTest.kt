package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.toOption
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.infra.helper.SeedData
import com.example.realworldkotlinspringbootjdbc.usecase.comment.CommentWithAuthor
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.text.SimpleDateFormat

class CommentWithAuthorsQueryModelImplTest {
    companion object {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("コメントに紐づくユーザーを取得")
    class FetchList {
        @BeforeEach
        fun reset() = DbConnection.resetSequence()

        @Test
        @DataSet(
            value = ["datasets/yml/given/articles.yml", "datasets/yml/given/users.yml"]
        )
        fun `正常系-未ログインのとき、指定したコメント全ての Author を取得できる`() {
            /**
             * given:
             * - コメント 3 件
             */
            val commentWithAuthorsQueryModel = CommentWithAuthorsQueryModelImpl(namedParameterJdbcTemplate)
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val comments = listOf(
                Comment.newWithoutValidation(
                    id = CommentId.newWithoutValidation(1),
                    body = Body.newWithoutValidation("dummy-comment-body-01"),
                    createdAt = date,
                    updatedAt = date,
                    authorId = UserId(3),
                ),
                Comment.newWithoutValidation(
                    id = CommentId.newWithoutValidation(3),
                    body = Body.newWithoutValidation("dummy-comment-body-03"),
                    createdAt = date,
                    updatedAt = date,
                    authorId = UserId(2),
                ),
                Comment.newWithoutValidation(
                    id = CommentId.newWithoutValidation(5),
                    body = Body.newWithoutValidation("dummy-comment-body-05"),
                    createdAt = date,
                    updatedAt = date,
                    authorId = UserId(3),
                ),
            )

            /**
             * when:
             */
            val actual = commentWithAuthorsQueryModel.fetchList(comments)

            /**
             * then:
             */
            val expected = listOf(
                CommentWithAuthor(
                    Comment.newWithoutValidation(
                        id = CommentId.newWithoutValidation(1),
                        body = Body.newWithoutValidation("dummy-comment-body-01"),
                        createdAt = date,
                        updatedAt = date,
                        authorId = UserId(3),
                    ),
                    OtherUser.newWithoutValidation(
                        userId = UserId(3),
                        username = Username.newWithoutValidation("graydon-hoare"),
                        bio = Bio.newWithoutValidation("Rustを作った"),
                        image = Image.newWithoutValidation(""),
                        following = false
                    )
                ),
                CommentWithAuthor(
                    Comment.newWithoutValidation(
                        id = CommentId.newWithoutValidation(3),
                        body = Body.newWithoutValidation("dummy-comment-body-03"),
                        createdAt = date,
                        updatedAt = date,
                        authorId = UserId(2),
                    ),
                    OtherUser.newWithoutValidation(
                        userId = UserId(2),
                        username = Username.newWithoutValidation("松本行弘"),
                        bio = Bio.newWithoutValidation("Rubyを作った"),
                        image = Image.newWithoutValidation(""),
                        following = false
                    )
                ),
                CommentWithAuthor(
                    Comment.newWithoutValidation(
                        id = CommentId.newWithoutValidation(5),
                        body = Body.newWithoutValidation("dummy-comment-body-02"),
                        createdAt = date,
                        updatedAt = date,
                        authorId = UserId(3),
                    ),
                    OtherUser.newWithoutValidation(
                        userId = UserId(3),
                        username = Username.newWithoutValidation("graydon-hoare"),
                        bio = Bio.newWithoutValidation("Rustを作った"),
                        image = Image.newWithoutValidation(""),
                        following = false
                    )
                )
            )
            when (actual) {
                is Either.Left -> assert(false)
                is Either.Right -> {
                    assertThat(actual.value[0].comment.id).isEqualTo(expected[0].comment.id)
                    assertThat(actual.value[0].comment.body).isEqualTo(expected[0].comment.body)
                    assertThat(actual.value[0].comment.authorId).isEqualTo(expected[0].comment.authorId)
                    assertThat(actual.value[0].author.userId).isEqualTo(expected[0].author.userId)
                    assertThat(actual.value[0].author.username).isEqualTo(expected[0].author.username)
                    assertThat(actual.value[0].author.bio).isEqualTo(expected[0].author.bio)
                    assertThat(actual.value[0].author.image).isEqualTo(expected[0].author.image)
                    assertThat(actual.value[0].author.following).isEqualTo(expected[0].author.following)

                    assertThat(actual.value[1].comment.id).isEqualTo(expected[1].comment.id)
                    assertThat(actual.value[1].comment.body).isEqualTo(expected[1].comment.body)
                    assertThat(actual.value[1].comment.authorId).isEqualTo(expected[1].comment.authorId)
                    assertThat(actual.value[1].author.userId).isEqualTo(expected[1].author.userId)
                    assertThat(actual.value[1].author.username).isEqualTo(expected[1].author.username)
                    assertThat(actual.value[1].author.bio).isEqualTo(expected[1].author.bio)
                    assertThat(actual.value[1].author.image).isEqualTo(expected[1].author.image)
                    assertThat(actual.value[1].author.following).isEqualTo(expected[1].author.following)

                    assertThat(actual.value[2].comment.id).isEqualTo(expected[2].comment.id)
                    assertThat(actual.value[2].comment.body).isEqualTo(expected[2].comment.body)
                    assertThat(actual.value[2].comment.authorId).isEqualTo(expected[2].comment.authorId)
                    assertThat(actual.value[2].author.userId).isEqualTo(expected[2].author.userId)
                    assertThat(actual.value[2].author.username).isEqualTo(expected[2].author.username)
                    assertThat(actual.value[2].author.bio).isEqualTo(expected[2].author.bio)
                    assertThat(actual.value[2].author.image).isEqualTo(expected[2].author.image)
                    assertThat(actual.value[2].author.following).isEqualTo(expected[2].author.following)
                }
            }
        }

        @Test
        @DataSet(
            value = ["datasets/yml/given/articles.yml", "datasets/yml/given/users.yml"]
        )
        fun `正常系-ログイン済のとき、指定したコメント全ての Author を取得され、Author がフォロイーだった場合、following が true になる`() {
            /**
             * given:
             * - フォロイーが存在するユーザー ID
             * - コメント 3 件
             */
            val commentWithAuthorsQueryModel = CommentWithAuthorsQueryModelImpl(namedParameterJdbcTemplate)
            val currentUser = SeedData.users().filter { it.userId.value == 3 }[0]
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val comments = listOf(
                Comment.newWithoutValidation(
                    id = CommentId.newWithoutValidation(1),
                    body = Body.newWithoutValidation("dummy-comment-body-01"),
                    createdAt = date,
                    updatedAt = date,
                    authorId = UserId(3),
                ),
                Comment.newWithoutValidation(
                    id = CommentId.newWithoutValidation(3),
                    body = Body.newWithoutValidation("dummy-comment-body-03"),
                    createdAt = date,
                    updatedAt = date,
                    authorId = UserId(2),
                ),
                Comment.newWithoutValidation(
                    id = CommentId.newWithoutValidation(5),
                    body = Body.newWithoutValidation("dummy-comment-body-05"),
                    createdAt = date,
                    updatedAt = date,
                    authorId = UserId(3),
                ),
            )

            /**
             * when:
             */
            val actual = commentWithAuthorsQueryModel.fetchList(comments, currentUser.toOption())

            /**
             * then:
             */
            val expected = listOf(
                CommentWithAuthor(
                    Comment.newWithoutValidation(
                        id = CommentId.newWithoutValidation(1),
                        body = Body.newWithoutValidation("dummy-comment-body-01"),
                        createdAt = date,
                        updatedAt = date,
                        authorId = UserId(3),
                    ),
                    OtherUser.newWithoutValidation(
                        userId = UserId(3),
                        username = Username.newWithoutValidation("graydon-hoare"),
                        bio = Bio.newWithoutValidation("Rustを作った"),
                        image = Image.newWithoutValidation(""),
                        following = false
                    )
                ),
                CommentWithAuthor(
                    Comment.newWithoutValidation(
                        id = CommentId.newWithoutValidation(3),
                        body = Body.newWithoutValidation("dummy-comment-body-03"),
                        createdAt = date,
                        updatedAt = date,
                        authorId = UserId(2),
                    ),
                    OtherUser.newWithoutValidation(
                        userId = UserId(2),
                        username = Username.newWithoutValidation("松本行弘"),
                        bio = Bio.newWithoutValidation("Rubyを作った"),
                        image = Image.newWithoutValidation(""),
                        following = true // フォローイーのため、true
                    )
                ),
                CommentWithAuthor(
                    Comment.newWithoutValidation(
                        id = CommentId.newWithoutValidation(5),
                        body = Body.newWithoutValidation("dummy-comment-body-02"),
                        createdAt = date,
                        updatedAt = date,
                        authorId = UserId(3),
                    ),
                    OtherUser.newWithoutValidation(
                        userId = UserId(3),
                        username = Username.newWithoutValidation("graydon-hoare"),
                        bio = Bio.newWithoutValidation("Rustを作った"),
                        image = Image.newWithoutValidation(""),
                        following = false
                    )
                )
            )
            when (actual) {
                is Either.Left -> assert(false)
                is Either.Right -> {
                    assertThat(actual.value[0].comment.id).isEqualTo(expected[0].comment.id)
                    assertThat(actual.value[0].comment.body).isEqualTo(expected[0].comment.body)
                    assertThat(actual.value[0].comment.authorId).isEqualTo(expected[0].comment.authorId)
                    assertThat(actual.value[0].author.userId).isEqualTo(expected[0].author.userId)
                    assertThat(actual.value[0].author.username).isEqualTo(expected[0].author.username)
                    assertThat(actual.value[0].author.bio).isEqualTo(expected[0].author.bio)
                    assertThat(actual.value[0].author.image).isEqualTo(expected[0].author.image)
                    assertThat(actual.value[0].author.following).isEqualTo(expected[0].author.following)

                    assertThat(actual.value[1].comment.id).isEqualTo(expected[1].comment.id)
                    assertThat(actual.value[1].comment.body).isEqualTo(expected[1].comment.body)
                    assertThat(actual.value[1].comment.authorId).isEqualTo(expected[1].comment.authorId)
                    assertThat(actual.value[1].author.userId).isEqualTo(expected[1].author.userId)
                    assertThat(actual.value[1].author.username).isEqualTo(expected[1].author.username)
                    assertThat(actual.value[1].author.bio).isEqualTo(expected[1].author.bio)
                    assertThat(actual.value[1].author.image).isEqualTo(expected[1].author.image)
                    assertThat(actual.value[1].author.following).isEqualTo(expected[1].author.following)

                    assertThat(actual.value[2].comment.id).isEqualTo(expected[2].comment.id)
                    assertThat(actual.value[2].comment.body).isEqualTo(expected[2].comment.body)
                    assertThat(actual.value[2].comment.authorId).isEqualTo(expected[2].comment.authorId)
                    assertThat(actual.value[2].author.userId).isEqualTo(expected[2].author.userId)
                    assertThat(actual.value[2].author.username).isEqualTo(expected[2].author.username)
                    assertThat(actual.value[2].author.bio).isEqualTo(expected[2].author.bio)
                    assertThat(actual.value[2].author.image).isEqualTo(expected[2].author.image)
                    assertThat(actual.value[2].author.following).isEqualTo(expected[2].author.following)
                }
            }
        }

        @Test
        fun `正常系-コメント 0 件の場合、空のリストが戻り値`() {
            /**
             * given:
             * - 空のリスト
             */
            val commentWithAuthorsQueryModel = CommentWithAuthorsQueryModelImpl(namedParameterJdbcTemplate)
            val comments = listOf<Comment>()

            /**
             * when:
             */
            val actual = commentWithAuthorsQueryModel.fetchList(comments)

            /**
             * then:
             */
            val expected = listOf<CommentWithAuthor>()
            when (actual) {
                is Either.Left -> assert(false)
                is Either.Right -> assertThat(actual.value).isEqualTo(expected)
            }
        }
    }
}
