package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Body as ArticleBody
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.usecase.article.ShowArticleUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.text.SimpleDateFormat
import java.util.stream.Stream

class ArticleControllerTest {
    class Show {
        private val requestHeader = "dummy-authorize"
        private val pathParam = "dummy-slug"

        private fun articleController(
            myAuth: MyAuth,
            showArticleUseCase: ShowArticleUseCase,
        ): ArticleController = ArticleController(myAuth, showArticleUseCase)

        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<ShowArticleUseCase.Error, CreatedArticle>,
            val expected: ResponseEntity<String>
        )

        @TestFactory
        fun test(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    title = "UseCase 成功:ユースケース（ShowArticleUseCase）が作成済記事（CreatedArticle）を返すとき、レスポンスのステータスコードが 200 になる",
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
                )
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    // when
                    val actual = articleController(
                        object : MyAuth {
                            override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                                return MyAuth.Unauthorized.RequiredBearerToken.left()
                            }
                        },
                        object : ShowArticleUseCase {
                            override fun execute(slug: String?): Either<ShowArticleUseCase.Error, CreatedArticle> =
                                testCase.useCaseExecuteResult
                        }
                    ).show(
                        rawAuthorizationHeader = requestHeader,
                        slug = pathParam
                    )
                    // then
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }
}