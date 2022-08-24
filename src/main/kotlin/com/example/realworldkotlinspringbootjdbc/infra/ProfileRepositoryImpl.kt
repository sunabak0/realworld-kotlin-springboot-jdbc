package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
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
    override fun show(username: Username, currentUserId: UserId): Either<ProfileRepository.ShowError, OtherUser> {
        val sql = """
            SELECT
                users.id
                , users.username
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
        val profileFromDb = try {
            namedParameterJdbcTemplate.queryForList(sql, sqlParams)
        } catch (e: Throwable) {
            return ProfileRepository.ShowError.NotFoundProfileByUsername(username, currentUserId).left()
        }

        /**
         * user が存在しなかった時 NotFoundError
         */
        if (profileFromDb.isEmpty()) {
            return ProfileRepository.ShowError.NotFoundProfileByUsername(username, currentUserId).left()
        }
        val it = profileFromDb.first()

        return try {
            OtherUser.newWithoutValidation(
                UserId(it["id"].toString().toInt()),
                Username.newWithoutValidation(it["username"].toString()),
                Bio.newWithoutValidation(it["bio"].toString()),
                Image.newWithoutValidation(it["image"].toString()),
                it["following_flg"].toString() == "1"
            ).right()
        } catch (e: Throwable) {
            ProfileRepository.ShowError.NotFoundProfileByUsername(username, currentUserId).left()
        }
    }

    override fun show(username: Username): Either<ProfileRepository.ShowWithoutAuthorizedError, OtherUser> {
        val sql = """
                SELECT
                    users.id
                    , users.username
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
        val profileFromDb = try {
            namedParameterJdbcTemplate.queryForList(sql, sqlParams)
        } catch (e: Throwable) {
            return ProfileRepository.ShowWithoutAuthorizedError.Unexpected(e, username).left()
        }

        /**
         * user が存在しなかった時 NotFoundError
         */
        if (profileFromDb.isEmpty()) {
            return ProfileRepository.ShowWithoutAuthorizedError.NotFoundProfileByUsername(username).left()
        }
        val it = profileFromDb.first()

        return try {
            OtherUser.newWithoutValidation(
                UserId(it["id"].toString().toInt()),
                Username.newWithoutValidation(it["username"].toString()),
                Bio.newWithoutValidation(it["bio"].toString()),
                Image.newWithoutValidation(it["image"].toString()),
                it["following_flg"].toString() == "1"
            ).right()
        } catch (e: Throwable) {
            ProfileRepository.ShowWithoutAuthorizedError.Unexpected(e, username).left()
        }
    }

    override fun follow(username: Username, currentUserId: UserId): Either<ProfileRepository.FollowError, OtherUser> {
        /**
         * user を取得
         */
        val selectUserSql = """
                SELECT
                    users.id
                    , users.username
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
        val sqlSelectUserSqlParams = MapSqlParameterSource()
            .addValue("username", username.value)
            .addValue("current_user_id", currentUserId.value)
        val profileFromDb = try {
            namedParameterJdbcTemplate.queryForList(selectUserSql, sqlSelectUserSqlParams)
        } catch (e: Throwable) {
            return ProfileRepository.FollowError.Unexpected(e, username, currentUserId).left()
        }

        /**
         * user が存在しなかった時 NotFoundError
         */
        if (profileFromDb.isEmpty()) {
            return ProfileRepository.FollowError.NotFoundProfileByUsername(username, currentUserId).left()
        }

        /**
         * 未フォローのとき、フォロー
         */
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
        try {
            namedParameterJdbcTemplate.update(insertFollowingsSql, insertFollowingsSqlParams)
        } catch (e: Throwable) {
            return ProfileRepository.FollowError.Unexpected(e, username, currentUserId).left()
        }

        val it = profileFromDb.first()
        return try {
            OtherUser.newWithoutValidation(
                UserId(it["id"].toString().toInt()),
                Username.newWithoutValidation(it["username"].toString()),
                Bio.newWithoutValidation(it["bio"].toString()),
                Image.newWithoutValidation(it["image"].toString()),
                true
            ).right()
        } catch (e: Throwable) {
            ProfileRepository.FollowError.Unexpected(e, username, currentUserId).left()
        }
    }

    override fun unfollow(
        username: Username,
        currentUserId: UserId
    ): Either<ProfileRepository.UnfollowError, OtherUser> {
        /**
         * user を取得
         */
        val selectUserSql = """
                SELECT
                    users.id
                    , users.username
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
        val sqlSelectUserSqlParams = MapSqlParameterSource()
            .addValue("username", username.value)
            .addValue("current_user_id", currentUserId.value)
        val profileFromDb = try {
            namedParameterJdbcTemplate.queryForList(selectUserSql, sqlSelectUserSqlParams)
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
         * フォロー済のとき、アンフォロー
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
        try {
            namedParameterJdbcTemplate.update(deleteFollowingsSql, deleteFollowingsSqlParams)
        } catch (e: Throwable) {
            return ProfileRepository.UnfollowError.Unexpected(e, username, currentUserId).left()
        }

        val it = profileFromDb.first()
        return try {
            OtherUser.newWithoutValidation(
                UserId(it["id"].toString().toInt()),
                Username.newWithoutValidation(it["username"].toString()),
                Bio.newWithoutValidation(it["bio"].toString()),
                Image.newWithoutValidation(it["image"].toString()),
                false
            ).right()
        } catch (e: Throwable) {
            ProfileRepository.UnfollowError.Unexpected(e, username, currentUserId).left()
        }
    }

    override fun findByUsername(
        username: Username,
        viewpointUserId: Option<UserId>
    ): Either<ProfileRepository.FindByUsernameError, OtherUser> {
        val sqlParams = MapSqlParameterSource().addValue("username", username.value)
        val userList = when (viewpointUserId) {
            /**
             * 登録済みユーザー検索
             * - followingを '0' で固定
             */
            None -> namedParameterJdbcTemplate.queryForList(
                """
                    SELECT
                        users.id
                        , users.email
                        , users.username
                        , users.password
                        , profiles.bio
                        , profiles.image
                        , '0' AS following_flg
                    FROM
                        users
                    JOIN
                        profiles
                    ON
                        profiles.user_id = users.id
                        AND users.username = :username
                    ;
                """.trimIndent(),
                sqlParams
            )
            /**
             * 特定の登録済みユーザーから見た登録済みユーザー検索
             * - followingが0 or 1
             */
            is Some -> namedParameterJdbcTemplate.queryForList(
                """
                    SELECT
                        users.id
                        , users.username
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
                        AND followings.follower_id = :viewpoint_user_id
                    ;
                """.trimIndent(),
                sqlParams.addValue("viewpoint_user_id", viewpointUserId.value.value)
            )
        }
        return when {
            /**
             * 見つからなかった
             */
            userList.isEmpty() -> ProfileRepository.FindByUsernameError.NotFound(username).left()
            /**
             * 見つかった
             */
            else -> {
                val user = userList.first()
                OtherUser.newWithoutValidation(
                    userId = UserId(user["id"].toString().toInt()),
                    username = Username.newWithoutValidation(user["username"].toString()),
                    bio = Bio.newWithoutValidation(user["bio"].toString()),
                    image = Image.newWithoutValidation(user["image"].toString()),
                    following = user["following_flg"].toString() == "1"
                ).right()
            }
        }
    }
}
