package com.example.realworldkotlinspringbootjdbc.usecase.comment

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.None
import arrow.core.Option
import arrow.core.left
import com.example.realworldkotlinspringbootjdbc.domain.CommentRepository
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

/**
 * 作成済記事のコメント取得
 *
 * - ログイン済みでリクエストした場合、author との followings が表示される
 */
interface ListCommentUseCase {
    /**
     * 実行
     *
     * @param slug Slug
     * @param currentUser リクエストユーザー or 未ログイン状態
     * @return エラー or Slug に該当する作成済み記事の一覧
     */
    fun execute(slug: String?, currentUser: Option<RegisteredUser> = None): Either<Error, List<CommentWithAuthor>> =
        throw NotImplementedError()

    sealed interface Error : MyError {
        data class InvalidSlug(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class NotFound(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class ListCommentUseCaseImpl(
    val commentRepository: CommentRepository,
    val commentWithAuthorsQueryModel: CommentWithAuthorsQueryModel
) : ListCommentUseCase {
    override fun execute(
        slug: String?,
        currentUser: Option<RegisteredUser>
    ): Either<ListCommentUseCase.Error, List<CommentWithAuthor>> {
        /**
         * Slug のバリデーション
         * Invalid -> 早期リターン
         */
        val validatedSlug = Slug.new(slug).fold(
            { return ListCommentUseCase.Error.InvalidSlug(it).left() },
            { it }
        )

        /**
         * コメントの取得
         * NotFoundArticleBySlug -> 早期リターン
         */
        val commentList = when (val listResult = commentRepository.list(validatedSlug)) {
            is Left -> when (val listError = listResult.value) {
                is CommentRepository.ListError.NotFoundArticleBySlug -> return ListCommentUseCase.Error.NotFound(
                    listError
                ).left()
            }

            is Right -> listResult
        }

        /**
         * comment の author を QueryModel で取得
         */
        return when (
            val commentWithAuthorResult =
                commentWithAuthorsQueryModel.fetchList(commentList.value, currentUser)
        ) {
            is Left -> throw UnsupportedOperationException("現在この分岐に入ることは無い")
            is Right -> commentWithAuthorResult
        }
    }
}
