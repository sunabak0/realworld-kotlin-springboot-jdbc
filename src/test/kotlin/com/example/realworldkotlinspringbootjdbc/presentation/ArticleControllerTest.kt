package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UpdatableCreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.article.Body
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
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

class ArticleControllerTest {

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
}
