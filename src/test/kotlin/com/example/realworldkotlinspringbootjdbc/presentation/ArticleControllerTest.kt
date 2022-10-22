package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.Option
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UpdatableCreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.article.Body
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.FeedParameters
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.infra.helper.SeedData
import com.example.realworldkotlinspringbootjdbc.usecase.article.CreateArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.DeleteCreatedArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.FeedUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.FilterCreatedArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.ShowArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.UpdateCreatedArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.shared_model.CreatedArticleWithAuthor
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.example.realworldkotlinspringbootjdbc.util.MyError
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.WithNull
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
                        object : DeleteCreatedArticleUseCase {},
                        object : UpdateCreatedArticleUseCase {},
                        object : FeedUseCase {},
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
                        object : DeleteCreatedArticleUseCase {},
                        object : UpdateCreatedArticleUseCase {},
                        object : FeedUseCase {},
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
                )
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
                            ): Either<CreateArticleUseCase.Error, CreatedArticleWithAuthor> =
                                testCase.useCaseExecuteResult
                        },
                        object : DeleteCreatedArticleUseCase {},
                        object : UpdateCreatedArticleUseCase {},
                        object : FeedUseCase {},
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

    class Delete {
        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<DeleteCreatedArticleUseCase.Error, Unit>,
            val expected: ResponseEntity<String>
        )

        @TestFactory
        fun test(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    title = "正常系-ユースケースの実行結果が成功の場合、ステータスコードが200のレスポンス",
                    useCaseExecuteResult = Unit.right(),
                    expected = ResponseEntity("", HttpStatus.valueOf(200))
                ),
                TestCase(
                    title = "準正常系-ユースケースの実行結果が「著者ではない」旨のエラーだった場合、ステータスコードが422のレスポンス",
                    useCaseExecuteResult = DeleteCreatedArticleUseCase.Error.NotAuthor(
                        cause = object : MyError {},
                        targetArticle = CreatedArticle.newWithoutValidation(
                            id = ArticleId(1),
                            title = Title.newWithoutValidation("fake-title"),
                            slug = Slug.newWithoutValidation("fake-slug"),
                            body = Body.newWithoutValidation("fake-body"),
                            createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                            updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                            description = Description.newWithoutValidation("fake-description"),
                            tagList = emptyList(),
                            authorId = UserId(1),
                            favorited = false,
                            favoritesCount = 0,
                        ),
                        notAuthorizedUser = RegisteredUser.newWithoutValidation(
                            userId = UserId(2), // targetArticleのauthorIdとは異なる箇所
                            email = Email.newWithoutValidation("fake-email@example.com"),
                            username = Username.newWithoutValidation("fake-username"),
                            bio = Bio.newWithoutValidation("fake-bio"),
                            image = Image.newWithoutValidation("fake-image")
                        )
                    ).left(),
                    expected = ResponseEntity("""{"errors":{"body":["削除する権限がありません"]}}""", HttpStatus.valueOf(422))
                ),
                TestCase(
                    title = "準正常系-ユースケースの実行結果が「記事が見つからなかった」旨のエラーだった場合、ステータスコードが422のレスポンス",
                    useCaseExecuteResult = DeleteCreatedArticleUseCase.Error.NotFoundArticle(
                        slug = Slug.newWithoutValidation("fake-slug")
                    ).left(),
                    expected = ResponseEntity("""{"errors":{"body":["記事が見つかりませんでした"]}}""", HttpStatus.valueOf(422))
                ),
                TestCase(
                    title = "準正常系-ユースケースの実行結果が「バリデーションエラー」という旨のエラーだった場合、ステータスコードが422のレスポンス",
                    useCaseExecuteResult = DeleteCreatedArticleUseCase.Error.ValidationError(
                        errors = listOf(Slug.ValidationError.Required)
                    ).left(),
                    expected = ResponseEntity(
                        """{"errors":{"body":[{"key":"Slug","message":"slugを入力してください。"}]}}""",
                        HttpStatus.valueOf(422)
                    )
                ),
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    val articleController = ArticleController(
                        object : MyAuth {
                            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> =
                                RegisteredUser.newWithoutValidation(
                                    UserId(1),
                                    Email.newWithoutValidation("dummy@example.com"),
                                    Username.newWithoutValidation("dummy-name"),
                                    Bio.newWithoutValidation("dummy-bio"),
                                    Image.newWithoutValidation("dummy-image"),
                                ).right()
                        },
                        object : ShowArticleUseCase {},
                        object : FilterCreatedArticleUseCase {},
                        object : CreateArticleUseCase {},
                        object : DeleteCreatedArticleUseCase {
                            override fun execute(
                                author: RegisteredUser,
                                slug: String?
                            ): Either<DeleteCreatedArticleUseCase.Error, Unit> =
                                testCase.useCaseExecuteResult
                        },
                        object : UpdateCreatedArticleUseCase {},
                        object : FeedUseCase {},
                    )

                    /**
                     * when:
                     */
                    val actual = articleController.delete("fake-token", "fake-slug")

                    /**
                     * then:
                     */
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }

        @Property
        fun `準正常系-認証に失敗する場合、ステータスコードが401のレスポンス`(
            @ForAll @WithNull fakeTokenString: String?,
            @ForAll @WithNull fakeSlugString: String?,
        ) {
            /**
             * given:
             * - 認証に失敗するコントローラー
             */
            val articleController = ArticleController(
                object : MyAuth {
                    override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> =
                        MyAuth.Unauthorized.FailedDecodeToken(
                            cause = object : MyError {},
                            token = "fake-token",
                        ).left()
                },
                object : ShowArticleUseCase {},
                object : FilterCreatedArticleUseCase {},
                object : CreateArticleUseCase {},
                object : DeleteCreatedArticleUseCase {},
                object : UpdateCreatedArticleUseCase {},
                object : FeedUseCase {},
            )

            /**
             * when:
             */
            val actual = articleController.delete(fakeSlugString, fakeTokenString)

            /**
             * then:
             */
            val expected = ResponseEntity("", HttpStatus.valueOf(401))
            assertThat(actual).isEqualTo(expected)
        }
    }

    class Update {
        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<UpdateCreatedArticleUseCase.Error, CreatedArticleWithAuthor>,
            val expected: ResponseEntity<String>
        )

        @TestFactory
        fun test(): Stream<DynamicNode> = Stream.of(
            TestCase(
                title = "正常系-記事の更新に成功した場合、ステータスコード200のレスポンスが戻り値",
                useCaseExecuteResult = CreatedArticleWithAuthor(
                    article = CreatedArticle.newWithoutValidation(
                        id = ArticleId(1),
                        title = Title.newWithoutValidation("更新後-プログラマーが知るべき97のこと"),
                        slug = Slug.newWithoutValidation("programmer-97"),
                        body = Body.newWithoutValidation("更新後-21. 技術的例外とビジネス例外を明確に区別する"),
                        createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                        updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                        description = Description.newWithoutValidation("更新後-エッセイ集"),
                        tagList = listOf(Tag.newWithoutValidation("essay"), Tag.newWithoutValidation("programming")),
                        authorId = UserId(1),
                        favorited = false,
                        favoritesCount = 0,
                    ),
                    author = OtherUser.newWithoutValidation(
                        userId = UserId(1),
                        username = Username.newWithoutValidation("Paul Graham"),
                        bio = Bio.newWithoutValidation("Lisper"),
                        image = Image.newWithoutValidation("img"),
                        following = false,
                    ),
                ).right(),
                expected = ResponseEntity(
                    """{"article":{"title":"更新後-プログラマーが知るべき97のこと","slug":"programmer-97","body":"更新後-21. 技術的例外とビジネス例外を明確に区別する","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","description":"更新後-エッセイ集","tagList":["essay","programming"],"authorId":1,"favorited":false,"favoritesCount":0}}""",
                    HttpStatus.valueOf(200)
                )
            ),
            TestCase(
                title = "準正常系-記事のバリデーションエラーが原因で更新に失敗した場合、ステータスコード422のレスポンスが戻り値",
                useCaseExecuteResult = UpdateCreatedArticleUseCase.Error.InvalidArticle(
                    errors = listOf(
                        UpdatableCreatedArticle.ValidationError.NothingAttributeToUpdatable
                    )
                ).left(),
                expected = ResponseEntity(
                    """{"errors":{"body":[{"key":"UpdatableCreatedArticle","message":"更新する項目が有りません"}]}}""",
                    HttpStatus.valueOf(422)
                )
            ),
            TestCase(
                title = "準正常系-Slugのバリデーションエラーが原因で更新に失敗した場合、ステータスコード422のレスポンスが戻り値",
                useCaseExecuteResult = UpdateCreatedArticleUseCase.Error.InvalidSlug(
                    errors = listOf(
                        Slug.ValidationError.TooLong("fake-1111-2222-3333-4444-5555-6666")
                    )
                ).left(),
                expected = ResponseEntity(
                    """{"errors":{"body":[{"slug":"fake-1111-2222-3333-4444-5555-6666","key":"Slug","message":"slugは32文字以下にしてください。"}]}}""",
                    HttpStatus.valueOf(422)
                )
            ),
            TestCase(
                title = "準正常系-著者ではないことが原因で更新に失敗した場合、ステータスコード422のレスポンスが戻り値",
                useCaseExecuteResult = UpdateCreatedArticleUseCase.Error.NotAuthor(
                    cause = object : MyError {},
                    targetArticle = CreatedArticle.newWithoutValidation(
                        id = ArticleId(1),
                        title = Title.newWithoutValidation("fake-title"),
                        slug = Slug.newWithoutValidation("fake-slug"),
                        body = Body.newWithoutValidation("fake-body"),
                        createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                        updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                        description = Description.newWithoutValidation("fake-description"),
                        tagList = listOf(Tag.newWithoutValidation("fake-tag")),
                        authorId = UserId(1),
                        favorited = false,
                        favoritesCount = 0,
                    ),
                    notAuthorizedUser = RegisteredUser.newWithoutValidation(
                        userId = UserId(2),
                        email = Email.newWithoutValidation("fake@example.com"),
                        username = Username.newWithoutValidation("fake-name"),
                        bio = Bio.newWithoutValidation("fake-bio"),
                        image = Image.newWithoutValidation("fake-image"),
                    )
                ).left(),
                expected = ResponseEntity("""{"errors":{"body":["削除する権限がありません"]}}""", HttpStatus.valueOf(403))
            ),
            TestCase(
                title = "準正常系-記事が見つからないことが原因で更新に失敗した場合、ステータスコード422のレスポンスが戻り値",
                useCaseExecuteResult = UpdateCreatedArticleUseCase.Error.NotFoundArticle(
                    slug = Slug.newWithoutValidation("fake-slug")
                ).left(),
                expected = ResponseEntity("""{"errors":{"body":["記事が見つかりません　"]}}""", HttpStatus.valueOf(404))
            )
        ).map { testCase ->
            dynamicTest(testCase.title) {
                /**
                 * given:
                 * - 認証は成功する
                 */
                val articleController = ArticleController(
                    object : MyAuth {
                        override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> =
                            RegisteredUser.newWithoutValidation(
                                UserId(1),
                                Email.newWithoutValidation("fake@example.com"),
                                Username.newWithoutValidation("fake-name"),
                                Bio.newWithoutValidation("fake-bio"),
                                Image.newWithoutValidation("fake-image"),
                            ).right()
                    },
                    object : ShowArticleUseCase {},
                    object : FilterCreatedArticleUseCase {},
                    object : CreateArticleUseCase {},
                    object : DeleteCreatedArticleUseCase {},
                    object : UpdateCreatedArticleUseCase {
                        override fun execute(
                            requestedUser: RegisteredUser,
                            slug: String?,
                            title: String?,
                            description: String?,
                            body: String?
                        ): Either<UpdateCreatedArticleUseCase.Error, CreatedArticleWithAuthor> =
                            testCase.useCaseExecuteResult
                    },
                    object : FeedUseCase {},
                )

                /**
                 * when:
                 */
                val actual = articleController.update(
                    rawAuthorizationHeader = "fake-auth-token",
                    slug = "fake-slug",
                    rawRequestBody = """{"article": {"title":"fake-title", "description":"fake-description", "body": "fake-body"}}""",
                )

                /**
                 * then:
                 */
                assertThat(actual).isEqualTo(testCase.expected)
            }
        }
    }

    class Feed {
        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<FeedUseCase.Error, FeedUseCase.FeedCreatedArticles>,
            val expected: ResponseEntity<String>
        )

        @TestFactory
        fun test(): Stream<DynamicNode> = Stream.of(
            TestCase(
                title = "正常系-取得に成功した場合、ステータスコードが200のレスポンス",
                useCaseExecuteResult = FeedUseCase.FeedCreatedArticles(
                    articles = sortedSetOf(
                        compareBy { it.article.id.value },
                        CreatedArticleWithAuthor(
                            article = SeedData.createdArticlesFromViewpointSet()[UserId(3)]!!.find { it.id == ArticleId(2) }!!,
                            author = SeedData.otherUsersFromViewpointSet()[UserId(3)]!!.find { it.userId == UserId(1) }!!,
                        ),
                        CreatedArticleWithAuthor(
                            article = SeedData.createdArticlesFromViewpointSet()[UserId(3)]!!.find { it.id == ArticleId(3) }!!,
                            author = SeedData.otherUsersFromViewpointSet()[UserId(3)]!!.find { it.userId == UserId(2) }!!,
                        ),
                    ),
                    articlesCount = 2
                ).right(),
                expected = ResponseEntity(
                    """{"articlesCount":2,"articles":[{"title":"Functional programming kotlin","slug":"functional-programming-kotlin","body":"dummy-body","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2022-01-01T15:00:00.000Z","description":"dummy-description","tagList":["kotlin"],"authorId":1,"favorited":true,"favoritesCount":1},{"title":"TDD(Type Driven Development)","slug":"tdd-type-driven-development","body":"dummy-body","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2022-01-02T15:00:00.000Z","description":"dummy-description","tagList":[],"authorId":2,"favorited":false,"favoritesCount":2}]}""",
                    HttpStatus.valueOf(200)
                )
            ),
            TestCase(
                title = "準正常系-フィードパラメータのバリデーションエラーが原因で失敗した場合、ステータスコードが422のレスポンス",
                useCaseExecuteResult = FeedUseCase.Error.FeedParameterValidationErrors(
                    errors = listOf(
                        FeedParameters.ValidationError.LimitError.RequireMaximumOrUnder(value = 101)
                    )
                ).left(),
                expected = ResponseEntity(
                    """{"errors":{"body":[{"value":101,"key":"LimitError","message":"100以下である必要があります"}]}}""",
                    HttpStatus.valueOf(422)
                )
            ),
            TestCase(
                title = "準正常系-フィードパラメータのOffeset値がフィード結果の総記事数を超えていたことが原因で失敗した場合、ステータスコードが422のレスポンス",
                useCaseExecuteResult = FeedUseCase.Error.OffsetOverCreatedArticlesCountError(
                    feedParameters = object : FeedParameters {
                        override val limit: Int get() = 20
                        override val offset: Int get() = 3
                    },
                    articlesCount = 2,
                ).left(),
                expected = ResponseEntity(
                    """{"errors":{"body":["offset値が作成済み記事の数を超えています(offset=3, articlesCount=2)"]}}""",
                    HttpStatus.valueOf(422)
                )
            ),
        ).map { testCase ->
            dynamicTest(testCase.title) {
                /**
                 * given: JWT 認証が成功する ArticleController
                 */
                val articleController = ArticleController(
                    object : MyAuth {
                        override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> =
                            SeedData.users().find { it.userId == UserId(3) }!!.right()
                    },
                    object : ShowArticleUseCase {},
                    object : FilterCreatedArticleUseCase {},
                    object : CreateArticleUseCase {},
                    object : DeleteCreatedArticleUseCase {},
                    object : UpdateCreatedArticleUseCase {},
                    object : FeedUseCase {
                        override fun execute(
                            currentUser: RegisteredUser,
                            limit: String?,
                            offset: String?
                        ): Either<FeedUseCase.Error, FeedUseCase.FeedCreatedArticles> =
                            testCase.useCaseExecuteResult
                    },
                )

                /**
                 * when:
                 */
                val actual = articleController.feed(
                    "fake-token",
                    "fake-limit",
                    "fake-offset"
                )

                /**
                 * then:
                 */
                assertThat(actual).isEqualTo(testCase.expected)
            }
        }
    }
}
