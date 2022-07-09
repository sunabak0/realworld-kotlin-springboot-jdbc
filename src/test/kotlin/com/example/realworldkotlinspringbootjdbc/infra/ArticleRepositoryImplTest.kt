package com.example.realworldkotlinspringbootjdbc.infra

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("WithLocalDb")
class ArticleRepositoryImplTest {
    @Nested
    class Tags() {
        @Test
        fun `タグ一覧取得に成功した場合、タグの一覧が戻り値となる`() {
            TODO("RepositoryImplを実装時に記述する")
        }
    }
}
