package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UnregisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UpdatableRegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository.FindByEmailWithPasswordError
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository.FindByUserIdError
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.Password
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.Date

typealias RegisteredWithPassword = Pair<RegisteredUser, Password>

@Repository
class UserRepositoryImpl(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) : UserRepository {
    sealed interface Error : MyError {
        data class Unexpected(override val cause: Throwable) : Error, MyError.MyErrorWithThrowable
        data class NotFoundByEmail(val email: Email) : Error, MyError.Basic
    }

    override fun register(user: UnregisteredUser): Either<UserRepository.RegisterError, RegisteredUser> {
        /**
         * Email と Username の数をそれぞれカウント
         */
        val sql = """
            SELECT
                COUNT(
                    CASE
                         WHEN
                             users.email = :email
                         THEN 1
                         ELSE NULL
                    END
                ) AS EMAIL_CNT
                , COUNT(
                    CASE
                         WHEN
                             users.username = :username
                         THEN 1
                         ELSE NULL
                    END
                ) AS USERNAME_CNT
            FROM
                users
            ;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource()
            .addValue("email", user.email.value)
            .addValue("username", user.username.value)
        val emailAndUsernameCountMap = namedParameterJdbcTemplate.queryForMap(sql, sqlParams)

        val (emailCount, usernameCount) = Pair(
            emailAndUsernameCountMap["email_cnt"].toString().toInt(),
            emailAndUsernameCountMap["username_cnt"].toString().toInt()
        )

        return when {
            /**
             * エラー: Email が既に使われている
             */
            0 < emailCount -> UserRepository.RegisterError.AlreadyRegisteredEmail(user.email).left()
            /**
             * エラー: Username が既に使われている
             */
            0 < usernameCount -> UserRepository.RegisterError.AlreadyRegisteredUsername(user.username).left()
            /**
             * ユーザー登録
             */
            else -> {
                val userId = registerTransactionApply(user)
                RegisteredUser.newWithoutValidation(
                    userId,
                    user.email,
                    user.username,
                    Bio.newWithoutValidation(""),
                    Image.newWithoutValidation("")
                ).right()
            }
        }
    }

    /**
     * ユーザー登録処理
     */
    @Transactional
    fun registerTransactionApply(user: UnregisteredUser): UserId {
        val sql1 = "INSERT INTO users(email, username, password, created_at, updated_at) VALUES (:email, :username, :password, :created_at, :updated_at) RETURNING id;"
        val sql2 = "INSERT INTO profiles(user_id, bio, image, created_at, updated_at) VALUES (:user_id, :bio, :image, :created_at, :updated_at);"
        val sqlParams = MapSqlParameterSource()
            .addValue("email", user.email.value)
            .addValue("password", user.password.value)
            .addValue("username", user.username.value)
            .addValue("created_at", Date())
            .addValue("updated_at", Date())
            .addValue("bio", "")
            .addValue("image", "")
        val userId = namedParameterJdbcTemplate.queryForMap(sql1, sqlParams)["id"] as Int
        sqlParams.addValue("user_id", userId)
        namedParameterJdbcTemplate.update(sql2, sqlParams)
        return UserId(userId)
    }

    override fun findByEmailWithPassword(email: Email): Either<FindByEmailWithPasswordError, RegisteredWithPassword> {
        val sql = """
            SELECT
                users.id
                , users.email
                , users.username
                , users.password
                , profiles.bio
                , profiles.image
            FROM
                users
            JOIN
                profiles
            ON
                profiles.user_id = users.id
                AND users.email = :email
            ;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource().addValue("email", email.value)
        val users = namedParameterJdbcTemplate.queryForList(sql, sqlParams)

        return when {
            /**
             * エラー: ユーザーが見つからなかった
             */
            users.isEmpty() -> FindByEmailWithPasswordError.NotFound(email).left()
            /**
             * ユーザーが見つかった
             */
            else -> {
                val userRecord = users.first()
                val user = RegisteredUser.newWithoutValidation(
                    UserId(userRecord["id"].toString().toInt()),
                    Email.newWithoutValidation(userRecord["email"].toString()),
                    Username.newWithoutValidation(userRecord["username"].toString()),
                    Bio.newWithoutValidation(userRecord["bio"].toString()),
                    Image.newWithoutValidation(userRecord["image"].toString()),
                )
                val password = Password.newWithoutValidation(userRecord["password"].toString())
                Pair(user, password).right()
            }
        }
    }

    override fun findByUserId(id: UserId): Either<UserRepository.FindByUserIdError, RegisteredUser> {
        val sql = """
            SELECT
                users.id
                , users.email
                , users.username
                , users.password
                , profiles.bio
                , profiles.image
            FROM
                users
            JOIN
                profiles
            ON
                profiles.user_id = users.id
                AND users.id = :user_id
            ;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource().addValue("user_id", id.value)
        val users = namedParameterJdbcTemplate.queryForList(sql, sqlParams)

        return when {
            /**
             * エラー: ユーザーが見つからなかった
             */
            users.isEmpty() -> FindByUserIdError.NotFound(id).left()
            /**
             * ユーザーが見つかった
             */
            else -> {
                val userRecord = users.first()
                RegisteredUser.newWithoutValidation(
                    UserId(userRecord["id"].toString().toInt()),
                    Email.newWithoutValidation(userRecord["email"].toString()),
                    Username.newWithoutValidation(userRecord["username"].toString()),
                    Bio.newWithoutValidation(userRecord["bio"].toString()),
                    Image.newWithoutValidation(userRecord["image"].toString()),
                ).right()
            }
        }
    }

    @Transactional
    override fun update(user: UpdatableRegisteredUser): Either<UserRepository.UpdateError, Unit> {
        /**
         * UserId と Email と Username の数をそれぞれカウント
         */
        val sql = """
            SELECT
                COUNT(
                    CASE
                         WHEN
                             users.id = :user_id
                         THEN 1
                         ELSE NULL
                    END
                ) AS USER_CNT
                , COUNT(
                    CASE
                         WHEN
                             users.id != :user_id
                             AND users.email = :email
                         THEN 1
                         ELSE NULL
                    END
                ) AS EMAIL_CNT
                , COUNT(
                    CASE
                         WHEN
                             users.id != :user_id
                             AND users.username = :username
                         THEN 1
                         ELSE NULL
                    END
                ) AS USERNAME_CNT
            FROM
                users
            ;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource()
            .addValue("user_id", user.userId.value)
            .addValue("email", user.email.value)
            .addValue("username", user.username.value)
        val countMap = namedParameterJdbcTemplate.queryForMap(sql, sqlParams)
        val userCount = countMap["user_cnt"].toString().toInt()
        val emailCount = countMap["email_cnt"].toString().toInt()
        val usernameCount = countMap["username_cnt"].toString().toInt()

        return when {
            /**
             * エラー: ユーザー が見つからなかった
             */
            userCount < 1 -> UserRepository.UpdateError.NotFound(user.userId).left()
            /**
             * エラー: Email が既に使われている
             */
            0 < emailCount -> UserRepository.UpdateError.AlreadyRegisteredEmail(user.email).left()
            /**
             * エラー: Username が既に使われている
             */
            0 < usernameCount -> UserRepository.UpdateError.AlreadyRegisteredUsername(user.username).left()
            /**
             * ユーザー 更新
             */
            else -> {
                val updatedCount = namedParameterJdbcTemplate.update(
                    """
                        UPDATE
                            users
                        SET
                            email = :email
                            , username = :username
                            , updated_at = :updated_at
                        WHERE
                            id = :user_id
                        ;
                        UPDATE
                            profiles
                        SET
                            bio = :bio
                            , image = :image
                            , updated_at = :updated_at
                        WHERE
                            user_id = :user_id
                        ;
                    """.trimIndent(),
                    MapSqlParameterSource()
                        .addValue("user_id", user.userId.value)
                        .addValue("email", user.email.value)
                        .addValue("username", user.username.value)
                        .addValue("bio", user.bio.value)
                        .addValue("image", user.image.value)
                        .addValue("updated_at", user.updatedAt)
                )
                when (updatedCount) {
                    /**
                     * エラー: ユーザーが見つからなかった
                     */
                    0 -> UserRepository.UpdateError.NotFound(user.userId).left()
                    /**
                     * 成功
                     */
                    else -> Unit.right()
                }
            }
        }
    }
}
