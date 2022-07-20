package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ArticleRepositoryImpl(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) : ArticleRepository {
    override fun tags(): Either<ArticleRepository.TagsError, List<Tag>> {
        val tagListSql = """
            SELECT
                name
            FROM
                tags
            ;
        """.trimIndent()
        return try {
            namedParameterJdbcTemplate.queryForList(tagListSql, MapSqlParameterSource())
                .map { Tag.newWithoutValidation(it["name"].toString()) }
                .right()
        } catch (e: Throwable) {
            ArticleRepository.TagsError.Unexpected(e).left()
        }
    }
}
