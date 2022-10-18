package com.example.realworldkotlinspringbootjdbc.sandbox.komapper

import com.example.realworldkotlinspringbootjdbc.infra.DbConnection
import com.example.realworldkotlinspringbootjdbc.infra.entity.User
import com.example.realworldkotlinspringbootjdbc.infra.entity._User
import com.example.realworldkotlinspringbootjdbc.infra.entity.user
import com.example.realworldkotlinspringbootjdbc.infra.helper.SeedData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.dryRun
import org.komapper.core.dsl.query.first
import org.komapper.dialect.postgresql.jdbc.PostgreSqlJdbcDialect
import org.komapper.jdbc.JdbcDatabase

class UserTest {
    @Test
    fun `User一覧を取得のSQLを確認`() {
        /**
         * given:
         */
        val u: _User = Meta.user

        /**
         * when:
         */
        val actual: String = QueryDsl.from(u).dryRun().sql

        /**
         * then:
         */
        val expected = """
            select t0_.id, t0_.email, t0_.username, t0_.password, t0_.created_at, t0_.updated_at from users as t0_
        """.trimIndent()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `User一覧を取得する`() {
        /**
         * given:
         */
        val database = JdbcDatabase(
            dataSource = DbConnection.dataSource(),
            dialect = PostgreSqlJdbcDialect()
        )
        val u: _User = Meta.user
        val query = QueryDsl.from(u).orderBy(u.id).first()

        /**
         * when:
         */
        val actual: User = database.runQuery { query }

        /**
         * then:
         */
        val firstUser = SeedData.users().first()
        assertThat(actual.id).isEqualTo(firstUser.userId.value)
        assertThat(actual.email).isEqualTo(firstUser.email.value)
        assertThat(actual.username).isEqualTo(firstUser.username.value)
    }
}
