package com.example.realworldkotlinspringbootjdbc.domain

import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.StringLength

class UncreatedArticleTest {
    @Property
    fun `UncreatedArticle を生成できる`(
        @ForAll @StringLength(max = 32) title: String,
        @ForAll @StringLength(max = 64) description: String,
        @ForAll @StringLength(max = 1024) body: String,
    ) {
    }
}
