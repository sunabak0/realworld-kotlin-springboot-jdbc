package com.example.realworldkotlinspringbootjdbc.infra

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import javax.sql.DataSource

object DbConnection {
    fun dataSource(): DataSource {
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = "jdbc:postgresql://127.0.0.1:5432/realworld-db"
        hikariConfig.username = "realworld-user"
        hikariConfig.password = "realworld-pass"
        hikariConfig.connectionTimeout = java.lang.Long.valueOf(500)
        hikariConfig.isAutoCommit = true
        hikariConfig.transactionIsolation = "TRANSACTION_READ_COMMITTED"
        hikariConfig.poolName = "realworldPool01"
        hikariConfig.maximumPoolSize = 10
        return HikariDataSource(hikariConfig)
    }
    val namedParameterJdbcTemplate = NamedParameterJdbcTemplate(dataSource())

    /**
     * DBのserial型の値を設定する
     *
     * この処理で解決したい課題
     * - DBRider等を利用して前提レコードを用意するとプログラムからINSERT時にserial型として指定しているidがバッティングする
     *
     * どのように解決するか
     * - 前提レコードのidは $sequenceValue 未満で設定する
     * - プログラムでINSERTするレコードは $sequenceValue+1 から始める
     *
     * 使い方
     * ```kt
     * @TestInstance(TestInstance.Lifecycle.PER_CLASS)
     * class Foo {
     *     @BeforeAll
     *     fun beforeAll() = DbConnection.resetSequence()
     * }
     * ```
     */
    fun resetSequence() {
        val sequenceValue = 10000
        val sql = """
            SELECT
                setval('users_id_seq', $sequenceValue)
                , setval('profiles_id_seq', $sequenceValue)
                , setval('followings_id_seq', $sequenceValue)
                , setval('tags_id_seq', $sequenceValue)
                , setval('article_tags_id_seq', $sequenceValue)
                , setval('comments_id_seq', $sequenceValue)
                , setval('article_comments_id_seq', $sequenceValue)
                , setval('favorites_id_seq', $sequenceValue)
            ;
        """.trimIndent()
        namedParameterJdbcTemplate.queryForList(sql, MapSqlParameterSource())
    }
}
