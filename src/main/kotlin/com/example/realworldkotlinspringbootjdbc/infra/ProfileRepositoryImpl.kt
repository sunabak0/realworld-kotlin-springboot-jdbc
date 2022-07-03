package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Profile
import com.example.realworldkotlinspringbootjdbc.domain.ProfileRepository
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ProfileRepositoryImpl(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) : ProfileRepository {
    override fun show(username: Username, currentUserId: UserId): Either<ProfileRepository.ShowError, Profile> {
        val sql = """
            SELECT
                users.username
                , profiles.bio
                , profiles.image
                , CASE WHEN followings.id IS NOT NULL THEN 1 ELSE 0 END AS following_flg
            FROM
                users
            JOIN
                profiles
            ON
                users.id = profiles.user_id
                AND users.username = :username
            LEFT OUTER JOIN
                followings
            ON
                followings.following_id = users.id
                AND followings.follower_id = :current_user_id
            ;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource().addValue("username", username.value)
            .addValue("current_user_id", currentUserId.value)
        return try {
            val profileFromDb = namedParameterJdbcTemplate.queryForList(sql, sqlParams)
            if (profileFromDb.isNotEmpty()) {
                profileFromDb.map {
                    Profile.newWithoutValidation(
                        Username.newWithoutValidation(it["username"].toString()),
                        Bio.newWithoutValidation(it["bio"].toString()),
                        Image.newWithoutValidation(it["image"].toString()),
                        it["following_flg"].toString() == "1"
                    )
                }[0].right()
            } else {
                ProfileRepository.ShowError.NotFoundProfileByUsername(username, currentUserId).left()
            }
        } catch (e: Throwable) {
            ProfileRepository.ShowError.Unexpected(e, username, currentUserId).left()
        }
    }

    override fun show(username: Username): Either<ProfileRepository.ShowWithoutAuthorizedError, Profile> {
        val sql = """
                SELECT
                    users.username
                    , profiles.bio
                    , profiles.image
                    , 0 AS following_flg
                FROM
                    users
                JOIN
                    profiles
                ON
                    users.id = profiles.user_id
                    AND users.username = :username
                ;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource().addValue("username", username.value)
        return try {
            val profileFromDb = namedParameterJdbcTemplate.queryForList(sql, sqlParams)
            if (profileFromDb.isNotEmpty()) {
                profileFromDb.map {
                    Profile.newWithoutValidation(
                        Username.newWithoutValidation(it["username"].toString()),
                        Bio.newWithoutValidation(it["bio"].toString()),
                        Image.newWithoutValidation(it["image"].toString()),
                        it["following_flg"].toString() == "1"
                    )
                }[0].right()
            } else {
                ProfileRepository.ShowWithoutAuthorizedError.NotFoundProfileByUsername(username).left()
            }
        } catch (e: Throwable) {
            ProfileRepository.ShowWithoutAuthorizedError.Unexpected(e, username).left()
        }
    }

    override fun follow(username: Username, currentUserId: UserId): Either<ProfileRepository.FollowError, Profile> {
        val selectUserSql = """
            SELECT
                users.username
                , profiles.bio
                , profiles.image
                , CASE WHEN followings.id IS NOT NULL THEN 1 ELSE 0 END AS following_flg
            FROM
                users
            JOIN
                profiles
            ON
                users.id = profiles.user_id
                AND users.username = :username
            LEFT OUTER JOIN
                followings
            ON
                followings.following_id = users.id
                AND followings.follower_id = :current_user_id
            ;
        """.trimIndent()
        val sqlSelectUserSqlParams = MapSqlParameterSource().addValue("username", username.value)
            .addValue("current_user_id", currentUserId.value)
        val insertFollowingsSql = """
            INSERT INTO followings
                (
                    following_id
                    , follower_id 
                    , created_at
                )
            SELECT
                users.id
                , :current_user_id
                , NOW()
            FROM
                users
            WHERE
                users.username = :username
                AND NOT EXISTS (
                    SELECT
                        1
                    FROM
                        followings
                    JOIN
                        users
                    ON
                        followings.following_id = users.id
                    WHERE
                        users.username = :username
                        AND followings.following_id = users.id
                        AND followings.follower_id = :current_user_id
                )
            ;
        """.trimIndent()
        val insertFollowingsSqlParams = MapSqlParameterSource()
            .addValue("username", username.value)
            .addValue("current_user_id", currentUserId.value)
        return try {
            val profileFromDb = namedParameterJdbcTemplate.queryForList(selectUserSql, sqlSelectUserSqlParams)
            when (profileFromDb.isEmpty()) {
                /**
                 * プロフィールが存在しなかったら、FollowError.NotFoundProfileByUsername
                 */
                true -> ProfileRepository.FollowError.NotFoundProfileByUsername(username, currentUserId).left()
                /**
                 * プロフィールが存在したら、フォロー
                 */
                false -> {
                    namedParameterJdbcTemplate.update(insertFollowingsSql, insertFollowingsSqlParams)
                    profileFromDb.map {
                        Profile.newWithoutValidation(
                            Username.newWithoutValidation(it["username"].toString()),
                            Bio.newWithoutValidation(it["bio"].toString()),
                            Image.newWithoutValidation(it["image"].toString()),
                            true
                        )
                    }[0].right()
                }
            }
        } catch (e: Throwable) {
            ProfileRepository.FollowError.Unexpected(e, username, currentUserId).left()
        }
    }

    override fun unfollow(username: Username, currentUserId: UserId): Either<ProfileRepository.UnfollowError, Profile> {
        val profileFromDb: MutableList<MutableMap<String, Any>>

        /**
         * username を取得
         */
        try {
            val selectUserSql = """
                SELECT
                    users.username
                    , profiles.bio
                    , profiles.image
                    , CASE WHEN followings.id IS NOT NULL THEN 1 ELSE 0 END AS following_flg
                FROM
                    users
                JOIN
                    profiles
                ON
                    users.id = profiles.user_id
                    AND users.username = :username
                LEFT OUTER JOIN
                    followings
                ON
                    followings.following_id = users.id
                    AND followings.follower_id = :current_user_id
                ;
            """.trimIndent()
            val sqlSelectUserSqlParams = MapSqlParameterSource().addValue("username", username.value)
                .addValue("current_user_id", currentUserId.value)
            profileFromDb = namedParameterJdbcTemplate.queryForList(selectUserSql, sqlSelectUserSqlParams)
        } catch (e: Throwable) {
            return ProfileRepository.UnfollowError.Unexpected(e, username, currentUserId).left()
        }

        /**
         * user が存在しなかった時 NotFoundError
         */
        if (profileFromDb.isEmpty()) {
            return ProfileRepository.UnfollowError.NotFoundProfileByUsername(username, currentUserId).left()
        }

        /**
         * user が存在した時 アンフォロー
         */
        val deleteFollowingsSql = """
            DELETE FROM
                followings
            USING
                users
            WHERE
                users.username = :username
                AND users.id = followings.following_id
                AND followings.follower_id = :current_user_id
            ;
        """.trimIndent()
        val deleteFollowingsSqlParams = MapSqlParameterSource()
            .addValue("username", username.value)
            .addValue("current_user_id", currentUserId.value)
        return try {
            namedParameterJdbcTemplate.update(deleteFollowingsSql, deleteFollowingsSqlParams)
            profileFromDb.map {
                Profile.newWithoutValidation(
                    Username.newWithoutValidation(it["username"].toString()),
                    Bio.newWithoutValidation(it["bio"].toString()),
                    Image.newWithoutValidation(it["image"].toString()),
                    false
                )
            }[0].right()
        } catch (e: Throwable) {
            ProfileRepository.UnfollowError.Unexpected(e, username, currentUserId).left()
        }
    }
}
