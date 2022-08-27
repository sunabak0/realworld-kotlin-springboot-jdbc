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
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat

class FilterCreatedArticleUseCaseTest {
    class Execute {
        /**
         * - 以下の2つのRepositoryがmockされたUseCase
         *   - 3つの作成済み記事を戻り値とするmockされたArticleRepository
         *   - 上記の作成済み記事を作成した著者(他ユーザー)郡を戻り値とするmockされたProfileRepository
         */
        private val mockedFilterUseCase = FilterCreatedArticleUseCaseImpl(
            articleRepository = object : ArticleRepository {
                override fun all(viewpointUserId: Option<UserId>): Either<ArticleRepository.AllError, List<CreatedArticle>> =
                    mockedCreatedArticleWithAuthorList.map { it.article }.toList().right()
            },
            profileRepository = object : ProfileRepository {
                override fun filterByUserIds(
                    userIds: Set<UserId>,
                    viewpointUserId: Option<UserId>
                ): Either<ProfileRepository.FilterByUserIdsError, Set<OtherUser>> =
                    mockedCreatedArticleWithAuthorList.map { it.author }.toSet().right()
            }
        )

        @Test
        fun `正常系-フィルタが無いと、 "offsetとlimitが効いた著者情報付きの作成済みの記事のリストとフィルタでひっかかった数" が戻り値`() {
            /**
             * given:
             * - Case1: offset=0, limit=1
             * - Case2: offset=2, limit=5
             */
            val case1Offset = 0
            val case1Limit = 1
            val case2Offset = 2
            val case2Limit = 5

            /**
             * when:
             */
            val case1Actual = mockedFilterUseCase.execute(
                offset = case1Offset.toString(),
                limit = case1Limit.toString()
            )
            val case2Actual = mockedFilterUseCase.execute(
                offset = case2Offset.toString(),
                limit = case2Limit.toString()
            )

            /**
             * when:
             * - Case1: limitが1なのでarticlesのサイズは1、見つかったカウントは3
             * - Case2: offsetが2なので、(0から数えて)2番目からからlimitが5なのでarticlesのサイズは1、見つかったカウントは3
             */
            val case1Expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                articles = listOf(
                    mockedCreatedArticleWithAuthorList[0]
                ),
                articlesCount = mockedCreatedArticleWithAuthorList.size
            ).right()
            val case2Expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                articles = listOf(
                    mockedCreatedArticleWithAuthorList[2]
                ),
                articlesCount = mockedCreatedArticleWithAuthorList.size
            ).right()
            assertThat(case1Actual).isEqualTo(case1Expected)
            assertThat(case2Actual).isEqualTo(case2Expected)
        }

        @Test
        fun `正常系-タグでフィルタすると、そのタグを持つ作成済み記事のみがひっかかった情報が戻り値`() {
            /**
             * given:
             * - Case1: 存在するタグ文字列
             * - Case2: 存在しないタグ文字列
             */
            val case1Tag = "essay"
            val case2Tag = "nothing"

            /**
             * when:
             */
            val case1Actual = mockedFilterUseCase.execute(tag = case1Tag)
            val case2Actual = mockedFilterUseCase.execute(tag = case2Tag)

            /**
             * then:
             * - Case1: 2つ引っかかる
             * - Case2: なにもない
             */
            val case1Expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                articles = listOf(
                    mockedCreatedArticleWithAuthorList[0],
                    mockedCreatedArticleWithAuthorList[1]
                ),
                articlesCount = 2
            ).right()
            val case2Expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                articles = emptyList(),
                articlesCount = 0
            ).right()
            assertThat(case1Actual).isEqualTo(case1Expected)
            assertThat(case2Actual).isEqualTo(case2Expected)
        }

        @Test
        fun `正常系-著者名でフィルタすると、その方が著者である作成済み記事がひっかかった情報が戻り値`() {
            /**
             * given:
             * - Case1: 存在する著者名
             * - Case2: 存在しない著者名
             */
            val case1AuthorString = "松本行弘"
            val case2AuthorString = "nobody"

            /**
             * when:
             */
            val case1Actual = mockedFilterUseCase.execute(author = case1AuthorString)
            val case2Actual = mockedFilterUseCase.execute(author = case2AuthorString)

            /**
             * then:
             * - Case1: 1つひっかかる
             * - Case2: なにもない
             */
            val case1Expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                articles = listOf(
                    mockedCreatedArticleWithAuthorList[1]
                ),
                articlesCount = 1
            ).right()
            val case2Expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                articles = emptyList(),
                articlesCount = 0
            ).right()
            assertThat(case1Actual).isEqualTo(case1Expected)
            assertThat(case2Actual).isEqualTo(case2Expected)
        }

        @Test
        fun `正常系-タグと著者名の両方でフィルタすると、両方ともひっかかる情報が戻り値`() {
            /**
             * given:
             * - Case1: どっちにもひっかかるフィルタ
             * - Case2: どっちかだけにしかひっかからないフィルタ
             */
            val case1AuthorString = "松本行弘"
            val case1TagString = "ruby"
            val case2AuthorString = "松本行弘"
            val case2TagString = "lisp"

            /**
             * when:
             */
            val case1Actual = mockedFilterUseCase.execute(author = case1AuthorString, tag = case1TagString)
            val case2Actual = mockedFilterUseCase.execute(author = case2AuthorString, tag = case2TagString)

            /**
             * then:
             * - Case1: 1つひっかかる
             * - Case2: なにもない
             */
            val case1Expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                articles = listOf(
                    mockedCreatedArticleWithAuthorList[1]
                ),
                articlesCount = 1
            ).right()
            val case2Expected = FilterCreatedArticleUseCase.FilteredCreatedArticleList(
                articles = emptyList(),
                articlesCount = 0
            ).right()
            assertThat(case1Actual).isEqualTo(case1Expected)
            assertThat(case2Actual).isEqualTo(case2Expected)
        }

        @Test
        fun `準正常系-offsetがフィルタ結果の数を超えていた場合、その旨のエラーが戻り値`() {
            /**
             * given:
             * - RepositoryがmockされたUseCase
             * - フィルタ結果の数を超えたoffset値
             */
            val overOffset = 5 // そもそも全体が3記事なので、どうあっても超える

            /**
             * when:
             */
            val actual = mockedFilterUseCase.execute(offset = overOffset.toString())

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
         * 1. UserId(1)が作成したタグ有り(lisp, essay)の記事
         * 2. UserId(2)が作成したタグ有り(essay, ruby)の記事
         * 3. UserId(1)が作成したタグ無しの記事
         */
        val mockedCreatedArticleWithAuthorList = listOf(
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
