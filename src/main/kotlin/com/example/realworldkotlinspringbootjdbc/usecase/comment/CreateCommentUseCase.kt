package com.example.realworldkotlinspringbootjdbc.usecase.comment

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.CommentRepository
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body
import com.example.realworldkotlinspringbootjdbc.usecase.shared_model.CommentWithAuthor
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

/**
 * 作成済み記事のコメントの作成
 */
interface CreateCommentUseCase {
    /**
     * 実行
     *
     * @param slug Slug
     * @param body コメントの本文
     * @param currentUser リクエストしたユーザー
     * @return エラー or 作成済み記事のコメント
     */
    fun execute(slug: String?, body: String?, currentUser: RegisteredUser): Either<Error, CommentWithAuthor> =
        throw NotImplementedError()

    sealed interface Error : MyError {
        data class InvalidSlug(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class InvalidCommentBody(override val errors: List<MyError.ValidationError>) :
            Error,
            MyError.ValidationErrors

        data class NotFound(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class CreateCommentUseCaseImpl(
    val commentRepository: CommentRepository
) : CreateCommentUseCase {
    override fun execute(
        slug: String?,
        body: String?,
        currentUser: RegisteredUser
    ): Either<CreateCommentUseCase.Error, CommentWithAuthor> {
        /**
         * Slug のバリデーション
         * Invalid -> 早期リターン
         */
        val validatedSlug = Slug.new(slug).fold(
            { return CreateCommentUseCase.Error.InvalidSlug(it).left() },
            { it }
        )

        /**
         * Body のバリデーション
         * Invalid -> 早期リターン
         */
        val commentBody = Body.new(body).fold(
            { return CreateCommentUseCase.Error.InvalidCommentBody(it).left() },
            { it }
        )

        return when (val createResult = commentRepository.create(validatedSlug, commentBody, currentUser.userId)) {
            /**
             * コメント登録 失敗
             */
            is Left -> when (val createError = createResult.value) {
                is CommentRepository.CreateError.NotFoundArticleBySlug -> CreateCommentUseCase.Error.NotFound(
                    createError
                ).left()
            }

            /**
             * コメント登録 成功
             */
            is Right -> CommentWithAuthor(
                createResult.value,
                OtherUser.newWithoutValidation(
                    userId = currentUser.userId,
                    username = currentUser.username,
                    bio = currentUser.bio,
                    image = currentUser.image,
                    following = false
                )
            ).right()
        }
    }
}
