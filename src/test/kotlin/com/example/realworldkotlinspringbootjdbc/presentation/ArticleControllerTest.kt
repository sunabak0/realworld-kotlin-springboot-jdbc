package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.Option
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
import com.example.realworldkotlinspringbootjdbc.usecase.article.ShowArticleUseCase
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

class ArticleControllerTest {
    class Show {
        private val requestHeader = "dummy-authorize"
        private val pathParam = "dummy-slug"

        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<ShowArticleUseCase.Error, CreatedArticle>,
            val expected: ResponseEntity<String>
        )

        val dummyRegisteredUser = RegisteredUser.newWithoutValidation(
            UserId(1),
            Email.newWithoutValidation("dummy@example.com"),
            Username.newWithoutValidation("dummy-name"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
        )

        @TestFactory
        fun testWithoutAuthorize(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    title = "UseCase 正常系:ユースケース（ShowArticleUseCase）が作成済記事（CreatedArticle）を返すとき、レスポンスのステータスコードが 200 になる",
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
                        """{"article":{"title":"dummy-title","slug":"dummy-slug","body":"dummy-body","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","description":"dummy-description","tagList":["dummy-tag1","dummy-tag2"],"authorId":1,"favorited":false,"favoritesCount":1}}""",
                        HttpStatus.valueOf(200)
                    )
                ),
                TestCase(
                    title = "UseCase 準正常系:ユースケース（ShowArticleUseCase）が NotFound エラー（NotFound）を返すとき、レスポンスのステータスコードが 404 になる",
                    useCaseExecuteResult = ShowArticleUseCase.Error.NotFoundArticleBySlug(
                        object : MyError {},
                        slug = Slug.newWithoutValidation("dummy-slug")
                    ).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":["記事が見つかりませんでした"]}}""",
                        HttpStatus.valueOf(404)
                    )
                ),
                TestCase(
                    title = "UseCase 準正常系:ユースケース（ShowArticleUseCase）がバリデーションエラー（ValidationErrors）を返すとき、レスポンスのステータスコードが 404 になる",
                    useCaseExecuteResult = ShowArticleUseCase.Error.ValidationErrors(
                        listOf(object : MyError.ValidationError {
                            override val message: String get() = "DummyValidationError"
                            override val key: String get() = "DummyKey"
                        })
                    ).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":["記事が見つかりませんでした"]}}""",
                        HttpStatus.valueOf(404)
                    )
                ),
                TestCase(
                    title = "UseCase 準正常系:ユースケース（ShowArticleUseCase）が原因不明のエラー（Unexpected）を返すとき、レスポンスのステータスコードが 500 になる",
                    useCaseExecuteResult = ShowArticleUseCase.Error.Unexpected(object : MyError {}).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":["原因不明のエラーが発生しました"]}}""",
                        HttpStatus.valueOf(500)
                    )
                )
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    /**
                     * given: JWT 認証が失敗する ArticleController
                     */
                    val articleController = ArticleController(
                        object : MyAuth { // JWT 認証が失敗する
                            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                                return MyAuth.Unauthorized.RequiredBearerToken.left()
                            }
                        },
                        object : ShowArticleUseCase {
                            override fun execute(
                                slug: String?,
                                currentUser: Option<RegisteredUser>
                            ): Either<ShowArticleUseCase.Error, CreatedArticle> =
                                testCase.useCaseExecuteResult
                        }
                    )
                    /**
                     * when:
                     */
                    val actual = articleController.show(rawAuthorizationHeader = null, slug = pathParam)
                    /**
                     * then:
                     */
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }

        @TestFactory
        fun testShowWithAuthorize(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    title = "UseCase 正常系:ユースケース（ShowArticleUseCase）が作成済記事（CreatedArticle）を返すとき、レスポンスのステータスコードが 200 になる",
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
                        favorited = true,
                        favoritesCount = 1
                    ).right(),
                    expected = ResponseEntity(
                        """{"article":{"title":"dummy-title","slug":"dummy-slug","body":"dummy-body","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","description":"dummy-description","tagList":["dummy-tag1","dummy-tag2"],"authorId":1,"favorited":true,"favoritesCount":1}}""",
                        HttpStatus.valueOf(200)
                    )
                ),
                TestCase(
                    title = "UseCase 準正常系:ユースケース（ShowArticleUseCase）が NotFound エラー（NotFound）を返すとき、レスポンスのステータスコードが 404 になる",
                    useCaseExecuteResult = ShowArticleUseCase.Error.NotFoundArticleBySlug(object : MyError {}, Slug.newWithoutValidation("dummy-slug")).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":["記事が見つかりませんでした"]}}""",
                        HttpStatus.valueOf(404)
                    )
                ),
                TestCase(
                    title = "UseCase 準正常系:ユースケース（ShowArticleUseCase）がバリデーションエラー（ValidationErrors）を返すとき、レスポンスのステータスコードが 404 になる",
                    useCaseExecuteResult = ShowArticleUseCase.Error.ValidationErrors(
                        listOf(object : MyError.ValidationError {
                            override val message: String get() = "DummyValidationError"
                            override val key: String get() = "DummyKey"
                        })
                    ).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":["記事が見つかりませんでした"]}}""",
                        HttpStatus.valueOf(404)
                    )
                ),
                TestCase(
                    title = "UseCase 準正常系:ユースケース（ShowArticleUseCase）が NotFoundUser エラー（NotFoundUser）を返すとき、レスポンスのステータスコードが 404 になる",
                    useCaseExecuteResult = ShowArticleUseCase.Error.NotFoundUser(object : MyError {}, dummyRegisteredUser).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":["ユーザー登録されていませんでした"]}}""",
                        HttpStatus.valueOf(404)
                    )
                ),
                TestCase(
                    title = "UseCase 準正常系:ユースケース（ShowArticleUseCase）が原因不明のエラー（Unexpected）を返すとき、レスポンスのステータスコードが 500 になる",
                    useCaseExecuteResult = ShowArticleUseCase.Error.Unexpected(object : MyError {}).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":["原因不明のエラーが発生しました"]}}""",
                        HttpStatus.valueOf(500)
                    )
                )
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    /**
                     * given: JWT 認証が成功する ArticleController
                     */
                    val articleController = ArticleController(
                        object : MyAuth {
                            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                                return dummyRegisteredUser.right()
                            }
                        },
                        object : ShowArticleUseCase {
                            override fun execute(
                                slug: String?,
                                currentUser: Option<RegisteredUser>
                            ): Either<ShowArticleUseCase.Error, CreatedArticle> =
                                testCase.useCaseExecuteResult
                        }
                    )
                    /**
                     * when
                     */
                    val actual = articleController.show(
                        rawAuthorizationHeader = requestHeader,
                        slug = pathParam
                    )
                    /**
                     * then
                     */
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }
}
