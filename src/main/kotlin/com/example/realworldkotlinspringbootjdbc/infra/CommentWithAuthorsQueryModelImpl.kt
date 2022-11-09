package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.usecase.comment.CommentWithAuthor
import com.example.realworldkotlinspringbootjdbc.usecase.comment.CommentWithAuthorsQueryModel
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.text.SimpleDateFormat

@Repository
class CommentWithAuthorsQueryModelImpl(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) :
    CommentWithAuthorsQueryModel {
    override fun fetchList(
        comments: List<Comment>,
        currentUser: Option<RegisteredUser>
    ): Either<CommentWithAuthorsQueryModel.FetchListError, List<CommentWithAuthor>> {
        /**
         * 作成済記事のコメントが 0 件だった場合、早期リターン
         */
        when (comments.isEmpty()) {
            true -> return listOf<CommentWithAuthor>().right()
            false -> {}
        }

        /**
         * ログイン状態によって、author の following の関係を取得するか分岐
         * - 未ログイン -> author.following = 0（未フォロー）固定
         * - ログイン済 -> author.following = 0（未フォロー）または 1（フォロー）
         */
        val commentWithAuthorsFromDb = when (currentUser) {
            /**
             * 未ログインのとき
             */
            is None -> {
                namedParameterJdbcTemplate.queryForList(
                    """
                        WITH other_users AS (
                            SELECT
                                users.id AS id
                                , users.username AS username
                                , profiles.bio AS bio
                                , profiles.image AS image
                                , 0 AS following_flg
                            FROM
                                users
                                JOIN
                                    profiles
                                ON
                                    profiles.user_id = users.id
                        )
                        SELECT
                            ac.id AS comment_id
                            , ac.body AS body
                            , ac.created_at AS created_at
                            , ac.updated_at AS updated_at
                            , ac.author_id AS author_id
                            , ou.id AS user_id
                            , ou.username AS username
                            , ou.bio AS bio
                            , ou.image AS image
                            , ou.following_flg AS following_flg
                        FROM
                            article_comments ac
                            JOIN
                                other_users ou
                            ON
                                ac.author_id = ou.id
                        WHERE
                            ac.id IN (:comment_ids)
                        ORDER BY 
                            ac.id
                        ;
                    """.trimIndent(),
                    MapSqlParameterSource().addValue("comment_ids", comments.map { it.id.value }.toSet())
                )
            }
            /**
             * ログイン済のとき
             */
            is Some -> {
                namedParameterJdbcTemplate.queryForList(
                    """
                        WITH other_users AS (
                            SELECT
                                users.id AS id
                                , users.username AS username
                                , profiles.bio AS bio
                                , profiles.image AS image
                                , CASE WHEN followings.id IS NOT NULL THEN 1 ELSE 0 END AS following_flg
                            FROM
                                users
                                JOIN
                                    profiles
                                ON
                                    profiles.user_id = users.id
                                LEFT OUTER JOIN
                                    followings
                                ON
                                    followings.following_id = users.id
                                    AND followings.follower_id = :curren_user_id
                        )
                        SELECT
                            ac.id AS comment_id
                            , ac.body AS body
                            , ac.created_at AS created_at
                            , ac.updated_at AS updated_at
                            , ac.author_id AS author_id
                            , ou.id AS user_id
                            , ou.username AS username
                            , ou.bio AS bio
                            , ou.image AS image
                            , ou.following_flg AS following_flg
                        FROM
                            article_comments ac
                            JOIN
                                other_users ou
                            ON
                                ac.author_id = ou.id
                        WHERE
                            ac.id IN (:comment_ids)
                        ORDER BY 
                            ac.id
                        ;
                    """.trimIndent(),
                    MapSqlParameterSource()
                        .addValue("comment_ids", comments.map { it.id.value }.toSet())
                        .addValue("curren_user_id", currentUser.value.userId.value)
                )
            }
        }

        val commentWithAuthors = commentWithAuthorsFromDb.map {
            CommentWithAuthor(
                Comment.newWithoutValidation(
                    id = CommentId.newWithoutValidation(it["comment_id"].toString().toInt()),
                    body = Body.newWithoutValidation(it["body"].toString()),
                    createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(it["created_at"].toString()),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(it["updated_at"].toString()),
                    authorId = UserId(it["author_id"].toString().toInt()),
                ),
                OtherUser.newWithoutValidation(
                    userId = UserId(it["user_id"].toString().toInt()),
                    username = Username.newWithoutValidation(it["username"].toString()),
                    bio = Bio.newWithoutValidation(it["bio"].toString()),
                    image = Image.newWithoutValidation(it["image"].toString()),
                    following = it["following_flg"].toString() == "1"
                )
            )
        }

        return commentWithAuthors.right()
    }
}
