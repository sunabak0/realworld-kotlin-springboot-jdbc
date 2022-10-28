package com.example.realworldkotlinspringbootjdbc.sandbox.komapper

import com.example.realworldkotlinspringbootjdbc.infra.DbConnection
import com.example.realworldkotlinspringbootjdbc.infra.entity.User
import com.example.realworldkotlinspringbootjdbc.infra.entity._User
import com.example.realworldkotlinspringbootjdbc.infra.entity.user
import com.example.realworldkotlinspringbootjdbc.infra.helper.SeedData
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.komapper.core.UniqueConstraintException
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.dryRun
import org.komapper.core.dsl.query.first
import org.komapper.dialect.postgresql.jdbc.PostgreSqlJdbcDialect
import org.komapper.jdbc.JdbcDatabase

class UserTest {
    @BeforeEach
    fun reset() = DbConnection.resetSequence()

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
    @DBRider
    @DataSet(
        value = [
            "datasets/yml/given/users.yml",
        ]
    )
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

    @Test
    @DBRider
    @DataSet(
        value = [
            "datasets/yml/given/empty-users.yml",
        ]
    )
    fun `Userを1行Insertすると、Userエンティティを返す`() {
        /**
         * given:
         */
        val database = JdbcDatabase(
            dataSource = DbConnection.dataSource(),
            dialect = PostgreSqlJdbcDialect()
        )
        val user = User(
            email = "test@example.com",
            password = "p@ssw0rd",
            username = "dummy-username"
        )
        val query = QueryDsl.insert(Meta.user).single(user)

        /**
         * when:
         */
        val actual: User = database.runQuery { query }

        /**
         * then:
         */
        assertThat(actual.email).isEqualTo(user.email)
        assertThat(actual.username).isEqualTo(user.username)
        assertThat(actual.password).isEqualTo(user.password)
    }

    @Test
    @DBRider
    @DataSet(
        value = [
            "datasets/yml/given/users.yml",
        ]
    )
    fun `emailが重複した時の例外を確認`() {
        /**
         * given:
         */
        val database = JdbcDatabase(
            dataSource = DbConnection.dataSource(),
            dialect = PostgreSqlJdbcDialect()
        )
        val user = User(
            email = SeedData.users().first().email.value,
            password = "p@ssw0rd",
            username = "dummy-username"
        )
        val query = QueryDsl.insert(Meta.user).single(user)

        /**
         * when:
         */
        val actual = assertThrows<UniqueConstraintException> {
            database.runQuery { query }
            Unit
        }.message

        /**
         * then:
         */
        val expected = """
            org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint "index_users_on_email"
              詳細: Key (email)=(paul-graham@example.com) already exists.
        """.trimIndent()
        assertThat(actual).isEqualTo(expected)
    }
}
