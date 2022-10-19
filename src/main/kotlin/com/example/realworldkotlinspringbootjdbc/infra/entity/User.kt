package com.example.realworldkotlinspringbootjdbc.infra.entity

import org.komapper.annotation.KomapperAutoIncrement
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Userエンティティ
 *
 * @property id
 * @property email
 * @property username
 * @property password
 * @property createdAt
 * @property updatedAt
 */
@KomapperEntity
@KomapperTable(
    name = "users"
)
data class User(
    @KomapperId @KomapperAutoIncrement
    val id: Int = 0,
    val email: String,
    val username: String,
    val password: String,
    @KomapperCreatedAt
    val createdAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.of("+09:00")),
    @KomapperUpdatedAt
    val updatedAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.of("+09:00")),
)
