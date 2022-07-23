package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.usecase.ListTagUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class DefaultControllerTest {
    @Nested
    @DisplayName("タグ一覧")
    class ListTest {
        @Test
        fun `成功-UseCase の実行結果が 'タグ一覧' だった場合、200 レスポンスを返す`() {
            // given:
            val listTagUseCase = object : ListTagUseCase {
                override fun execute(): Either<ListTagUseCase.Error, List<Tag>> =
                    listOf(
                        Tag.newWithoutValidation("dummy-tag-01"),
                        Tag.newWithoutValidation("dummy-tag-02"),
                    ).right()
            }
            val controller = DefaultController(listTagUseCase)

            // when:
            val actual = controller.list()

            // then:
            val expected = ResponseEntity(
                """{"tags":["dummy-tag-01","dummy-tag-02"]}""",
                HttpStatus.valueOf(200)
            )
            assertThat(actual).isEqualTo(expected)
        }
    }
}
