package com.example.realworldkotlinspringbootjdbc.usecase

import arrow.core.Either
import arrow.core.Option
import arrow.core.left
import arrow.core.orNull
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.article.Body
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.FilterParameters
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

        private class OffsetAndLimitTestCase(
            val title: String,
            val offset: String,
            val limit: String,
            val expected: Either<FilterCreatedArticleUseCase.Error, FilterCreatedArticleUseCase.FilteredCreatedArticleList>,
        )
        @TestFactory
        fun testOffsetAndLimit(): Stream<DynamicNode> =
            Stream.of(
                OffsetAndLimitTestCase(
                    title = "正常系-フィルタ結果から、0番目から数えてoffset番目から最大limit分だけが取得できる",
                    offset = 0.toString(),
                    limit = 1.toString(),
                    expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                        articles = listOf(
                            createdArticleWithAuthorListStubResult[0]
                        ),
                        articlesCount = createdArticleWithAuthorListStubResult.size
                    ).right()
                ),
                OffsetAndLimitTestCase(
                    title = "正常系-limitに余裕があっても最大limit分だけが取得できる",
                    offset = 2.toString(),
                    limit = 100.toString(),
                    expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                        articles = listOf(
                            createdArticleWithAuthorListStubResult[2]
                        ),
                        articlesCount = createdArticleWithAuthorListStubResult.size
                    ).right()
                ),
                OffsetAndLimitTestCase(
                    title = "準正常系-offsetがフィルタ結果の全体の数より大きい場合、エラーが戻り値",
                    offset = 100.toString(),
                    limit = 1.toString(),
                    expected = FilterCreatedArticleUseCase.Error.OffsetOverCreatedArticlesCountError(
                        filterParameters = FilterParameters.new(
                            offset = 100.toString(),
                            limit = 1.toString()
                        ).orNull()!!,
                        articlesCount = createdArticleWithAuthorListStubResult.size
                    ).left()
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

        private class TagFilterOnlyTestCase(
            val title: String,
            val filterTagString: String,
            val expected: Either<Nothing, FilterCreatedArticleUseCase.FilteredCreatedArticleList>,
        )
        @TestFactory
        fun testTagFilter(): Stream<DynamicNode> =
            Stream.of(
                TagFilterOnlyTestCase(
                    title = "正常系-存在するタグの場合、そのタグを持つ作成済み記事のみがひっかかる",
                    filterTagString = "essay",
                    expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                        articles = listOf(
                            createdArticleWithAuthorListStubResult[0],
                            createdArticleWithAuthorListStubResult[1]
                        ),
                        articlesCount = 2
                    ).right()
                ),
                TagFilterOnlyTestCase(
                    title = "正常系-存在しないタグの場合、1つもひっかからない",
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

        private class AuthorUsernameFilterOnlyTestCase(
            val title: String,
            val filterAuthorUsernameString: String,
            val expected: Either<Nothing, FilterCreatedArticleUseCase.FilteredCreatedArticleList>,
        )
        @TestFactory
        fun testAuthorUsernameFilter(): Stream<DynamicNode> =
            Stream.of(
                AuthorUsernameFilterOnlyTestCase(
                    title = "正常系-存在する著者名だった場合、その著者が投稿した作成済み記事のみがひっかかる",
                    filterAuthorUsernameString = "松本行弘",
                    expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                        articles = listOf(
                            createdArticleWithAuthorListStubResult[1]
                        ),
                        articlesCount = 1
                    ).right()
                ),
                AuthorUsernameFilterOnlyTestCase(
                    title = "正常系-存在しない著者名だった場合、1つもひっかからない",
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
                     * - 指定するのはauthorのみ
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

        private class TagFilterAndAuthorUsernameFilterTestCase(
            val title: String,
            val filterTagString: String,
            val filterAuthorUsernameString: String,
            val expected: Either<Nothing, FilterCreatedArticleUseCase.FilteredCreatedArticleList>,
        )
        @TestFactory
        fun testTagFilterAndAuthorFilter(): Stream<DynamicNode> =
            Stream.of(
                TagFilterAndAuthorUsernameFilterTestCase(
                    title = "正常系-タグと著者名の2つの場合、(ANDフィルタなので)どちらにも該当する作成済み記事のみがひっかかる",
                    filterTagString = "ruby",
                    filterAuthorUsernameString = "松本行弘",
                    expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                        articles = listOf(
                            createdArticleWithAuthorListStubResult[1]
                        ),
                        articlesCount = 1
                    ).right()
                ),
                TagFilterAndAuthorUsernameFilterTestCase(
                    title = "正常系-どっちかだけにしかひっかからないタグと著者名の場合、(ANDフィルタなので)1つもひっかからない",
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
                     * - 指定するのはtag,authorの2つ
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
