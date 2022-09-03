package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.Option
import arrow.core.left
import arrow.core.none
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UncreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.article.Body
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.FilterParameters
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.usecase.article.CreateArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.FilterCreatedArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.ShowArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.shared_model.CreatedArticleWithAuthor
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
                    title = "正常系-ユースケース（ShowArticleUseCase）が作成済記事（CreatedArticle）を返すとき、レスポンスのステータスコードが 200 になる",
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
                    title = "準正常系-ユースケース（ShowArticleUseCase）が NotFound エラー（NotFound）を返すとき、レスポンスのステータスコードが 404 になる",
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
                    title = "準正常系-ユースケース（ShowArticleUseCase）がバリデーションエラー（ValidationErrors）を返すとき、レスポンスのステータスコードが 404 になる",
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
                    title = "準正常系-ユースケース（ShowArticleUseCase）が原因不明のエラー（Unexpected）を返すとき、レスポンスのステータスコードが 500 になる",
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
                        },
                        object : FilterCreatedArticleUseCase {},
                        object : CreateArticleUseCase {},
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
                    title = "正常系-ユースケース（ShowArticleUseCase）が作成済記事（CreatedArticle）を返すとき、レスポンスのステータスコードが 200 になる",
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
                    title = "準正常系-ユースケース（ShowArticleUseCase）が NotFound エラー（NotFound）を返すとき、レスポンスのステータスコードが 404 になる",
                    useCaseExecuteResult = ShowArticleUseCase.Error.NotFoundArticleBySlug(
                        cause = object : MyError {},
                        slug = Slug.newWithoutValidation("dummy-slug")
                    ).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":["記事が見つかりませんでした"]}}""",
                        HttpStatus.valueOf(404)
                    )
                ),
                TestCase(
                    title = "準正常系-ユースケース（ShowArticleUseCase）がバリデーションエラー（ValidationErrors）を返すとき、レスポンスのステータスコードが 404 になる",
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
                    title = "準正常系-ユースケース（ShowArticleUseCase）が NotFoundUser エラー（NotFoundUser）を返すとき、レスポンスのステータスコードが 404 になる",
                    useCaseExecuteResult = ShowArticleUseCase.Error.NotFoundUser(
                        cause = object : MyError {},
                        user = dummyRegisteredUser
                    ).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":["ユーザー登録されていませんでした"]}}""",
                        HttpStatus.valueOf(404)
                    )
                ),
                TestCase(
                    title = "準正常系-ユースケース（ShowArticleUseCase）が原因不明のエラー（Unexpected）を返すとき、レスポンスのステータスコードが 500 になる",
                    useCaseExecuteResult = ShowArticleUseCase.Error.Unexpected(cause = object : MyError {}).left(),
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
                        },
                        object : FilterCreatedArticleUseCase {},
                        object : CreateArticleUseCase {},
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

    class Filter {
        private class TestCase(
            val title: String,
            val useCaseResult: Either<FilterCreatedArticleUseCase.Error, FilterCreatedArticleUseCase.FilteredCreatedArticleList>,
            val expected: ResponseEntity<String>
        )

        @TestFactory
        fun testShowWithAuthorize(): Stream<DynamicNode> =
            Stream.of(
                TestCase(
                    title = "正常系-フィルタが成功した場合、ステータスコードが200のレスポンス",
                    useCaseResult = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                        articles = listOf(
                            CreatedArticleWithAuthor(
                                article = CreatedArticle.newWithoutValidation(
                                    id = ArticleId(1),
                                    title = Title.newWithoutValidation("title-01"),
                                    slug = Slug.newWithoutValidation("slug-01"),
                                    body = Body.newWithoutValidation("body-01"),
                                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                                    description = Description.newWithoutValidation("description-01"),
                                    tagList = listOf(Tag.newWithoutValidation("tag-01"), Tag.newWithoutValidation("tag-02")),
                                    authorId = UserId(1),
                                    favorited = false,
                                    favoritesCount = 1,
                                ),
                                author = OtherUser.newWithoutValidation(
                                    userId = UserId(1),
                                    username = Username.newWithoutValidation("username-01"),
                                    bio = Bio.newWithoutValidation("bio-01"),
                                    image = Image.newWithoutValidation("image-01"),
                                    following = false
                                )
                            ),
                            CreatedArticleWithAuthor(
                                article = CreatedArticle.newWithoutValidation(
                                    id = ArticleId(2),
                                    title = Title.newWithoutValidation("title-02"),
                                    slug = Slug.newWithoutValidation("slug-02"),
                                    body = Body.newWithoutValidation("body-02"),
                                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                                    description = Description.newWithoutValidation("description-02"),
                                    tagList = listOf(Tag.newWithoutValidation("tag-03")),
                                    authorId = UserId(2),
                                    favorited = false,
                                    favoritesCount = 1,
                                ),
                                author = OtherUser.newWithoutValidation(
                                    userId = UserId(2),
                                    username = Username.newWithoutValidation("username-02"),
                                    bio = Bio.newWithoutValidation("bio-02"),
                                    image = Image.newWithoutValidation("image-02"),
                                    following = false
                                )
                            )
                        ),
                        articlesCount = 2,
                    ).right(),
                    expected = ResponseEntity(
                        """{"articlesCount":2,"articles":[{"title":"title-01","slug":"slug-01","body":"body-01","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","description":"description-01","tagList":["tag-01","tag-02"],"authorId":1,"favorited":false,"favoritesCount":1},{"title":"title-02","slug":"slug-02","body":"body-02","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","description":"description-02","tagList":["tag-03"],"authorId":2,"favorited":false,"favoritesCount":1}]}""",
                        HttpStatus.valueOf(200)
                    )
                ),
                TestCase(
                    title = "準正常系-フィルタパラメータのバリデーションエラーだった場合、ステータスコードが422のレスポンス",
                    useCaseResult = FilterCreatedArticleUseCase.Error.FilterParametersValidationErrors(
                        errors = listOf(
                            FilterParameters.ValidationError.LimitError.RequireMaximumOrUnder(999),
                            FilterParameters.ValidationError.OffsetError.FailedConvertToInteger("foo"),
                        )
                    ).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":[{"value":999,"key":"LimitError","message":"100以下である必要があります"},{"value":"foo","key":"LimitError","message":"数値に変換できる数字にしてください"}]}}""",
                        HttpStatus.valueOf(422)
                    )
                ),
                TestCase(
                    title = "準正常系-ユーザーが見つからなかった旨のエラーだった場合、ステータスコードが404のレスポンス",
                    useCaseResult = FilterCreatedArticleUseCase.Error.NotFoundUser(
                        user = RegisteredUser.newWithoutValidation(
                            userId = UserId(1),
                            email = Email.newWithoutValidation("fake@example.com"),
                            username = Username.newWithoutValidation("username-fake"),
                            bio = Bio.newWithoutValidation("bio-fake"),
                            image = Image.newWithoutValidation("image-fake")
                        ),
                        cause = object : MyError {}
                    ).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":["ユーザー登録されていませんでした"]}}""",
                        HttpStatus.valueOf(404)
                    )
                ),
                TestCase(
                    title = "準正常系-offset値がフィルタ後の作成済み記事の数を超えている旨のエラーだった場合、ステータスコードが422のレスポンス",
                    useCaseResult = FilterCreatedArticleUseCase.Error.OffsetOverCreatedArticlesCountError(
                        filterParameters = object : FilterParameters {
                            override val tag: Option<String> get() = none()
                            override val author: Option<String> get() = none()
                            override val favoritedByUsername: Option<String> get() = none()
                            override val limit: Int get() = 10
                            override val offset: Int get() = 999
                        },
                        articlesCount = 20
                    ).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":["offset値がフィルタした結果の作成済み記事の数を超えています(offset=999, articlesCount=20)"]}}""",
                        HttpStatus.valueOf(422)
                    )
                ),
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    /**
                     * given:
                     * - JWT認証の結果を固定
                     * - UseCaseの結果を固定
                     */
                    val articleController = ArticleController(
                        object : MyAuth { // JWT 認証が失敗する
                            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> =
                                MyAuth.Unauthorized.RequiredBearerToken.left()
                        },
                        object : ShowArticleUseCase {},
                        object : FilterCreatedArticleUseCase {
                            override fun execute(
                                tag: String?,
                                author: String?,
                                favoritedByUsername: String?,
                                limit: String?,
                                offset: String?,
                                currentUser: Option<RegisteredUser>
                            ): Either<FilterCreatedArticleUseCase.Error, FilterCreatedArticleUseCase.FilteredCreatedArticleList> =
                                testCase.useCaseResult // UseCaseの結果を固定
                        },
                        object : CreateArticleUseCase {},
                    )

                    /**
                     * when:
                     */
                    val actual = articleController.filter()

                    /**
                     * then:
                     */
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
    }

    class Create {
        private val requestHeader = "dummy-authorize"

        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<CreateArticleUseCase.Error, CreatedArticleWithAuthor>,
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
        fun test(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    title = "正常系-ユースケース（CreateArticleUseCase）が著者情報付き作成済記事（CreatedArticleWithAuthor）を返すとき、レスポンスのステータスコードが 200 になる",
                    useCaseExecuteResult = CreatedArticleWithAuthor(
                        article = CreatedArticle.newWithoutValidation(
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
                        ),
                        author = OtherUser.newWithoutValidation(
                            userId = UserId(1),
                            username = Username.newWithoutValidation("dummy-username"),
                            bio = Bio.newWithoutValidation("dummy-bio"),
                            image = Image.newWithoutValidation("dummy-image"),
                            following = false,
                        )
                    ).right(),
                    expected = ResponseEntity(
                        """{"article":{"title":"dummy-title","slug":"dummy-slug","body":"dummy-body","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","description":"dummy-description","tagList":["dummy-tag1","dummy-tag2"],"authorId":1,"favorited":true,"favoritesCount":1}}""",
                        HttpStatus.valueOf(200)
                    )
                ),
                TestCase(
                    title = "準正常系-ユースケース（CreateArticleUseCase）がバリデーションエラーを返すとき、レスポンスのステータスコードが 422 になる",
                    useCaseExecuteResult = CreateArticleUseCase.Error.InvalidArticle(
                        listOf(
                            object : MyError.ValidationError {
                                override val message: String get() = "dummy-message"
                                override val key: String get() = "dummy-key"
                            }
                        )
                    ).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":[{"key":"dummy-key","message":"dummy-message"}]}}""",
                        HttpStatus.valueOf(422)
                    )
                ),
                TestCase(
                    title = "準正常系-ユースケース（CreateArticleUseCase）が「既にタイトルは使用されています」を返すとき、レスポンスのステータスコードが 422 になる",
                    useCaseExecuteResult = CreateArticleUseCase.Error.AlreadyCreatedArticle(
                        cause = object : MyError {},
                        article = object : UncreatedArticle {
                            override val title: Title get() = Title.newWithoutValidation("dummy-title")
                            override val slug: Slug get() = Slug.newWithoutValidation("dummy-slug")
                            override val description: Description get() = Description.newWithoutValidation("dummy-description")
                            override val body: ArticleBody get() = ArticleBody.newWithoutValidation("dummy-body")
                            override val tagList: List<Tag>
                                get() = listOf(
                                    Tag.newWithoutValidation("dummy-tag1"),
                                    Tag.newWithoutValidation("dummy-tag2")
                                )
                            override val authorId: UserId get() = UserId(1)
                        }
                    ).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":["タイトルは既に使用されています。"]}}""",
                        HttpStatus.valueOf(422)
                    )
                ),
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    /**
                     * given:
                     */
                    val articleController = ArticleController(
                        object : MyAuth {
                            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                                return dummyRegisteredUser.right()
                            }
                        },
                        object : ShowArticleUseCase {},
                        object : FilterCreatedArticleUseCase {},
                        object : CreateArticleUseCase {
                            override fun execute(
                                currentUser: RegisteredUser,
                                title: String?,
                                description: String?,
                                body: String?,
                                tagList: List<String>?
                            ): Either<CreateArticleUseCase.Error, CreatedArticleWithAuthor> = testCase.useCaseExecuteResult
                        },
                    )

                    /**
                     * when:
                     */
                    val actual = articleController.create(
                        rawAuthorizationHeader = requestHeader,
                        rawRequestBody = """{"article": {"title":"dummy-title", "description":"dummy-description", "body": "dummy-body"}}"""
                    )

                    /**
                     * then:
                     */
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }
}
