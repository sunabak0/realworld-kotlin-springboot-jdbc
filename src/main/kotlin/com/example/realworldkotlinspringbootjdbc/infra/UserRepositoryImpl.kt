package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
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
import com.example.realworldkotlinspringbootjdbc.infra.entity.Profile
import com.example.realworldkotlinspringbootjdbc.infra.entity.User
import com.example.realworldkotlinspringbootjdbc.infra.entity.profile
import com.example.realworldkotlinspringbootjdbc.infra.entity.user
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.expression.When
import org.komapper.core.dsl.operator.alias
import org.komapper.core.dsl.operator.case
import org.komapper.core.dsl.operator.count
import org.komapper.core.dsl.operator.literal
import org.komapper.core.dsl.query.first
import org.komapper.core.dsl.query.firstOrNull
import org.komapper.jdbc.JdbcDatabase
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

typealias RegisteredWithPassword = Pair<RegisteredUser, Password>

@Repository
class UserRepositoryImpl(
    val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    val jdbcDatabase: JdbcDatabase,
) : UserRepository {
    companion object {
        /**
         * email, usernameの重複確認用クエリ
         *
         * @param email
         * @param username
         */
        fun emailAndUsernameMatchedCountQuery(email: Email, username: Username) =
            QueryDsl.from(Meta.user).selectNotNull(
                count(case(When({ Meta.user.email eq email.value }, literal(1)))).alias("email_cnt"),
                count(case(When({ Meta.user.username eq username.value }, literal(1)))).alias("username_cnt")
            )

        fun findByEmailWithPasswordQuery(email: Email) =
            QueryDsl.from(Meta.user).innerJoin(Meta.profile) {
                Meta.profile.userId eq Meta.user.id
                and { Meta.user.email eq email.value }
            }.select(
                Meta.user.id,
                Meta.user.email,
                Meta.user.username,
                Meta.user.password,
                Meta.profile.bio,
                Meta.profile.image,
            )
    }

    override fun register(user: UnregisteredUser): Either<UserRepository.RegisterError, RegisteredUser> {
        /**
         * Email と Username の数をそれぞれカウント
         */
        val (emailCount, usernameCount) = jdbcDatabase.runQuery {
            emailAndUsernameMatchedCountQuery(user.email, user.username).first()
        }

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
        val userEntity = jdbcDatabase.runQuery {
            QueryDsl.insert(Meta.user).single(
                User(
                    email = user.email.value,
                    password = user.password.value,
                    username = user.username.value
                )
            )
        }
        jdbcDatabase.runQuery {
            QueryDsl.insert(Meta.profile).single(
                Profile(
                    userId = userEntity.id,
                    bio = "",
                    image = "",
                )
            )
        }
        return UserId(userEntity.id)
    }

    override fun findByEmailWithPassword(email: Email): Either<FindByEmailWithPasswordError, RegisteredWithPassword> {
        val userRecord = jdbcDatabase.runQuery {
            findByEmailWithPasswordQuery(email).firstOrNull()
        }.toOption().fold(
            /**
             * 見つからなかった
             */
            { return FindByEmailWithPasswordError.NotFound(email).left() },
            /**
             * 見つかった
             */
            { it }
        )

        val user = RegisteredUser.newWithoutValidation(
            UserId(userRecord[Meta.user.id].toString().toInt()),
            Email.newWithoutValidation(userRecord[Meta.user.email].toString()),
            Username.newWithoutValidation(userRecord[Meta.user.username].toString()),
            Bio.newWithoutValidation(userRecord[Meta.profile.bio].toString()),
            Image.newWithoutValidation(userRecord[Meta.profile.image].toString()),
        )
        val password = Password.newWithoutValidation(userRecord[Meta.user.password].toString())
        return Pair(user, password).right()
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
