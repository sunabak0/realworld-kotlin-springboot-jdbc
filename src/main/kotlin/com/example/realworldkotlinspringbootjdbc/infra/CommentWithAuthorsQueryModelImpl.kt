package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
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
    override fun fetchList(comments: List<Comment>): Either<CommentWithAuthorsQueryModel.FetchListError, List<CommentWithAuthor>> {
        val selectCommentWithAuthorSql = """
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
        """.trimIndent()
        val commentWithAuthors = namedParameterJdbcTemplate.queryForList(
            selectCommentWithAuthorSql,
            MapSqlParameterSource().addValue("comment_ids", comments.map { it.id.value }.toSet())
        ).map {
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
                    following = false
                )
            )
        }
        return commentWithAuthors.right()
    }
}
