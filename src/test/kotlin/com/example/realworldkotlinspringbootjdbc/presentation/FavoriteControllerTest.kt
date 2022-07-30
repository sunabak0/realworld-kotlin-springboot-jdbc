package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.usecase.favorite.FavoriteUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.text.SimpleDateFormat
import java.util.stream.Stream
import com.example.realworldkotlinspringbootjdbc.domain.article.Body as ArticleBody

class FavoriteControllerTest {
    class Favorite {
        private val requestHeader = "dummy-authorize"
        private val pathParamSlug = "dummy-slug"
        val dummyRegisteredUser = RegisteredUser.newWithoutValidation(
            UserId(1),
            Email.newWithoutValidation("dummy@example.com"),
            Username.newWithoutValidation("dummy-name"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
        )

        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<FavoriteUseCase.Error, CreatedArticle>,
            val expected: ResponseEntity<String>,
        )

        @TestFactory
        fun test(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    title = "成功: FavoriteUseCase が作成済記事（FavoriteArticle）を返す場合、200 レスポンスを返す",
                    useCaseExecuteResult = CreatedArticle.newWithoutValidation(
                        id = ArticleId(1),
                        title = Title.newWithoutValidation("dummy-title"),
                        slug = Slug.newWithoutValidation("dummy-slug"),
                        body = ArticleBody.newWithoutValidation("dummy-body"),
                        createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                        updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                        description = Description.newWithoutValidation("dummy-description"),
                        tagList = listOf(
                            Tag.newWithoutValidation("dummy-tag1"),
                            Tag.newWithoutValidation("dummy-tag2")
                        ),
                        authorId = UserId(1),
                        favorited = false,
                        favoritesCount = 1
                    ).right(),
                    expected = ResponseEntity(
                        """{"article":{"title":"dummy-title","slug":"dummy-slug","body":"dummy-body","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","description":"dummy-description","tagList":["dummy-tag1","dummy-tag2"],"author":"1","favorited":false,"favoritesCount":1}}""",
                        HttpStatus.valueOf(200)
                    ),
                ),
                TestCase(
                    title = "失敗: FavoriteUseCase が「不正なSlug（InvalidSlug）」エラーを返す場合、404 レスポンスを返す",
                    useCaseExecuteResult = FavoriteUseCase.Error.InvalidSlug(
                        listOf(
                            object : MyError.ValidationError {
                                override val message: String get() = "DummyValidationError because Invalid Slug"
                                override val key: String get() = "DummyKey"
                            }
                        )
                    ).left(),
                    expected = ResponseEntity("""{"errors":{"body":["記事が見つかりませんでした"]}}""", HttpStatus.valueOf(404)),
                ),
                TestCase(
                    title = "失敗: FavoriteUseCase が「slug に該当する記事が見つからなかった（ArticleNotFound）」エラーを返す場合、404レスポンスを返す",
                    useCaseExecuteResult = FavoriteUseCase.Error.ArticleNotFoundBySlug(
                        object : MyError {},
                        Slug.newWithoutValidation(pathParamSlug)
                    ).left(),
                    expected = ResponseEntity("""{"errors":{"body":["記事が見つかりませんでした"]}}""", HttpStatus.valueOf(404)),
                ),
                TestCase(
                    title = "失敗: FavoriteUseCase が「原因不明（Unexpected）」エラーを返す場合、500 レスポンスを返す",
                    useCaseExecuteResult = FavoriteUseCase.Error.Unexpected(object : MyError {}).left(),
                    expected = ResponseEntity("""{"errors":{"body":["原因不明のエラーが発生しました"]}}""", HttpStatus.valueOf(500)),
                )
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    // given
                    val controller = FavoriteController(
                        object : MyAuth {
                            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                                return dummyRegisteredUser.right()
                            }
                        },
                        object : FavoriteUseCase {
                            override fun execute(
                                slug: String?,
                                currentUser: RegisteredUser
                            ): Either<FavoriteUseCase.Error, CreatedArticle> {
                                return testCase.useCaseExecuteResult
                            }
                        },
                    )

                    // when
                    val actual = controller.favorite(rawAuthorizationHeader = requestHeader, slug = pathParamSlug)

                    // then
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }
}
