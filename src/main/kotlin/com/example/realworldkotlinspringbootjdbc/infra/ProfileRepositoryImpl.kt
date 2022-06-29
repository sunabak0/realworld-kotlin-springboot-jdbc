package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Profile
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ProfileRepositoryImpl(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) : ProfileRepository {
    override fun show(username: Username): Either<ProfileRepository.ShowError, Profile> {
        val sql = """
            SELECT
                users.username
                , profiles.bio
                , profiles.image
            FROM
                users
            JOIN
                profiles
            ON
                users.id = profiles.user_id
            WHERE
                username = :username;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource()
            .addValue("username", username.value)
        return try {
            val profileFromDb = namedParameterJdbcTemplate.queryForList(sql, sqlParams)
            if (profileFromDb.isNotEmpty()) {
                profileFromDb.map {
                    Profile.newWithoutValidation(
                        Username.newWithoutValidation(it["username"].toString()),
                        Bio.newWithoutValidation(it["bio"].toString()),
                        Image.newWithoutValidation(it["image"].toString()),
                        false
                    )
                }[0].right()
            } else {
                ProfileRepository.ShowError.NotFoundProfileByUsername(username).left()
            }
        } catch (e: Throwable) {
            ProfileRepository.ShowError.Unexpected(e, username).left()
        }
    }
}
