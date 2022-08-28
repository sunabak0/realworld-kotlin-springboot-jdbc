package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Option
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.article.Body
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.usecase.article.FilterCreatedArticleUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.article.FilterCreatedArticleUseCaseImpl
import com.example.realworldkotlinspringbootjdbc.usecase.shared_model.CreatedArticleWithAuthor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.text.SimpleDateFormat
import java.util.stream.Stream

class FilterCreatedArticleUseCaseTest {
    class Execute {
        /**
         * - 以下の2つのRepositoryがstubされたUseCase
         *   - 3つの作成済み記事を戻り値とするstubされたArticleRepository
         *   - 上記の作成済み記事を作成した著者(他ユーザー)郡を戻り値とするstubされたProfileRepository
         */
        private val filterUseCaseStub = FilterCreatedArticleUseCaseImpl(
            articleRepository = object : ArticleRepository {
                override fun all(viewpointUserId: Option<UserId>): Either<ArticleRepository.AllError, List<CreatedArticle>> =
                    createdArticleWithAuthorListStubResult.map { it.article }.toList().right()
            },
            profileRepository = object : ProfileRepository {
                override fun filterByUserIds(
                    userIds: Set<UserId>,
                    viewpointUserId: Option<UserId>
                ): Either<ProfileRepository.FilterByUserIdsError, Set<OtherUser>> =
                    createdArticleWithAuthorListStubResult.map { it.author }.toSet().right()
            }
        )

        private class TestCase(
            val title: String,
            val filterTagString: String? = null,
            val filterAuthorUsernameString: String? = null,
            val offset: String? = null,
            val limit: String? = null,
            val expected: Either<Nothing, FilterCreatedArticleUseCase.FilteredCreatedArticleList>,
        )

        @TestFactory
        fun testOffsetAndLimit(): Stream<DynamicNode> =
            Stream.of(
                TestCase(
                    title = "正常系-フィルタ結果から、0番目から数えてoffset番目から最大limit分だけ取得できる",
                    offset = 0.toString(),
                    limit = 1.toString(),
                    expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                        articles = listOf(
                            createdArticleWithAuthorListStubResult[0]
                        ),
                        articlesCount = createdArticleWithAuthorListStubResult.size
                    ).right()
                ),
                TestCase(
                    title = "正常系-フィルタ結果から、0番目から数えてoffset番目から最大limit分だけ取得できる",
                    offset = 2.toString(),
                    limit = 5.toString(),
                    expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                        articles = listOf(
                            createdArticleWithAuthorListStubResult[2]
                        ),
                        articlesCount = createdArticleWithAuthorListStubResult.size
                    ).right()
                )
            ).map { testCase ->
                DynamicTest.dynamicTest(testCase.title) {
                    /**
                     * given:
                     */

                    /**
                     * when:
                     */
                    val actual = filterUseCaseStub.execute(
                        offset = testCase.offset,
                        limit = testCase.limit,
                    )

                    /**
                     * then:
                     */
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }

        @TestFactory
        fun testTagFilter(): Stream<DynamicNode> =
            Stream.of(
                TestCase(
                    title = "正常系-存在するタグでフィルタすると、そのタグを持つ作成済み記事のみがひっかかった情報が戻り値",
                    filterTagString = "essay",
                    expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                        articles = listOf(
                            createdArticleWithAuthorListStubResult[0],
                            createdArticleWithAuthorListStubResult[1]
                        ),
                        articlesCount = 2
                    ).right()
                ),
                TestCase(
                    title = "正常系-存在しないタグでフィルタすると、1つもひっかからなかったことがわかる情報が戻り値",
                    filterTagString = "nothing",
                    expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                        articles = emptyList(),
                        articlesCount = 0
                    ).right()
                )
            ).map { testCase ->
                DynamicTest.dynamicTest(testCase.title) {
                    /**
                     * given:
                     */

                    /**
                     * when:
                     */
                    val actual = filterUseCaseStub.execute(
                        tag = testCase.filterTagString
                    )

                    /**
                     * then:
                     */
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }

        @TestFactory
        fun testAuthorFilter(): Stream<DynamicNode> =
            Stream.of(
                TestCase(
                    title = "正常系-存在する著者名でフィルタすると、その著者が投稿した作成済み記事のみがひっかかった情報が戻り値",
                    filterAuthorUsernameString = "松本行弘",
                    expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                        articles = listOf(
                            createdArticleWithAuthorListStubResult[1]
                        ),
                        articlesCount = 1
                    ).right()
                ),
                TestCase(
                    title = "正常系-存在しない著者名でフィルタすると、1つもひっかからなかったことがわかる情報が戻り値",
                    filterAuthorUsernameString = "nobody",
                    expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                        articles = emptyList(),
                        articlesCount = 0
                    ).right()
                )
            ).map { testCase ->
                DynamicTest.dynamicTest(testCase.title) {
                    /**
                     * given:
                     */

                    /**
                     * when:
                     */
                    val actual = filterUseCaseStub.execute(
                        author = testCase.filterAuthorUsernameString
                    )

                    /**
                     * then:
                     */
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }

        @TestFactory
        fun testTagFilterAndAuthorFilter(): Stream<DynamicNode> =
            Stream.of(
                TestCase(
                    title = "正常系-タグと著者名でフィルタすると、(ANDフィルタなので)両方ともでひっかかる情報が戻り値",
                    filterTagString = "ruby",
                    filterAuthorUsernameString = "松本行弘",
                    expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                        articles = listOf(
                            createdArticleWithAuthorListStubResult[1]
                        ),
                        articlesCount = 1
                    ).right()
                ),
                TestCase(
                    title = "正常系-どっちかだけにしかひっかからないタグと著者名でフィルタすると、(ANDフィルタなので)1つもひっかからなかったことがわかる情報が戻り値",
                    filterTagString = "lisp",
                    filterAuthorUsernameString = "松本行弘",
                    expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                        articles = emptyList(),
                        articlesCount = 0
                    ).right()
                )
            ).map { testCase ->
                DynamicTest.dynamicTest(testCase.title) {
                    /**
                     * given:
                     */

                    /**
                     * when:
                     */
                    val actual = filterUseCaseStub.execute(
                        tag = testCase.filterTagString,
                        author = testCase.filterAuthorUsernameString
                    )

                    /**
                     * then:
                     */
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }

        @Test
        fun `準正常系-offsetがフィルタ結果の数を超えていた場合、その旨のエラーが戻り値`() {
            /**
             * given:
             * - RepositoryがstubされたUseCase
             * - フィルタ結果の数を超えたoffset値
             */
            val overOffset = 5 // そもそも全体が3記事なので、どうあっても超える

            /**
             * when:
             */
            val actual = filterUseCaseStub.execute(offset = overOffset.toString())

            /**
             * then:
             * - フィルタした作成済み記事の数よりoffset値が超えた旨のエラーがわかる
             * - フィルタした作成済み記事の数もわかる
             */
            when (actual) {
                is Left -> when (val error = actual.value) {
                    is FilterCreatedArticleUseCase.Error.OffsetOverCreatedArticlesCountError -> {
                        assertThat(error.articlesCount).isEqualTo(3)
                    }
                    else -> assert(false) { "原因: ${error.javaClass}" }
                }
                is Right -> assert(false) { "原因: ${actual.value}" }
            }
        }

        /**
         * 3つの作成済み記事With著者データが要素となるリスト
         *
         * - UserId(1)が作成したタグ有り(lisp, essay)の記事
         * - UserId(2)が作成したタグ有り(essay, ruby)の記事
         * - UserId(1)が作成したタグ無しの記事
         */
        val createdArticleWithAuthorListStubResult = listOf(
            CreatedArticleWithAuthor(
                article = CreatedArticle.newWithoutValidation(
                    id = ArticleId(1),
                    title = Title.newWithoutValidation("fake-title1"),
                    slug = Slug.newWithoutValidation("fake-slug1"),
                    body = Body.newWithoutValidation("fake-body1"),
                    description = Description.newWithoutValidation("fake-description1"),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    tagList = listOf(
                        Tag.newWithoutValidation("lisp"),
                        Tag.newWithoutValidation("essay"),
                    ),
                    authorId = UserId(1),
                    favorited = false,
                    favoritesCount = 1
                ),
                author = OtherUser.newWithoutValidation(
                    userId = UserId(1),
                    username = Username.newWithoutValidation("paul-graham"),
                    bio = Bio.newWithoutValidation("fake-bio1"),
                    image = Image.newWithoutValidation("fake-image1"),
                    following = false
                )
            ),
            CreatedArticleWithAuthor(
                article = CreatedArticle.newWithoutValidation(
                    id = ArticleId(2),
                    title = Title.newWithoutValidation("fake-title2"),
                    slug = Slug.newWithoutValidation("fake-slug2"),
                    body = Body.newWithoutValidation("fake-body2"),
                    description = Description.newWithoutValidation("fake-description2"),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    tagList = listOf(
                        Tag.newWithoutValidation("essay"),
                        Tag.newWithoutValidation("ruby"),
                    ),
                    authorId = UserId(2),
                    favorited = false,
                    favoritesCount = 2
                ),
                author = OtherUser.newWithoutValidation(
                    userId = UserId(2),
                    username = Username.newWithoutValidation("松本行弘"),
                    bio = Bio.newWithoutValidation("fake-bio1"),
                    image = Image.newWithoutValidation("fake-image1"),
                    following = false
                )
            ),
            CreatedArticleWithAuthor(
                article = CreatedArticle.newWithoutValidation(
                    id = ArticleId(3),
                    title = Title.newWithoutValidation("fake-title3"),
                    slug = Slug.newWithoutValidation("fake-slug3"),
                    body = Body.newWithoutValidation("fake-body3"),
                    description = Description.newWithoutValidation("fake-description3"),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                    tagList = listOf(),
                    authorId = UserId(1),
                    favorited = false,
                    favoritesCount = 3
                ),
                author = OtherUser.newWithoutValidation(
                    userId = UserId(1),
                    username = Username.newWithoutValidation("paul-graham"),
                    bio = Bio.newWithoutValidation("fake-bio1"),
                    image = Image.newWithoutValidation("fake-image1"),
                    following = false
                )
            ),
        )
    }
}
