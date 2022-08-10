package com.example.realworldkotlinspringbootjdbc.usecase.article

import arrow.core.Either
import arrow.core.Option
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticlesWithMetadata
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.FilterParameters
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

interface FilterCreatedArticleUseCase {
    /**
     * 作成済み記事をフィルタ(フィルタがなければただの一覧取得)
     *
     * @param tag タグでフィルタ(1つだけ、複数ではできない)
     * @param author 作成済み記事の著者でフィルタ(ユーザー名, 1人分だけ)
     * @param favoritedByUsernamea 特定の登録済みユーザーがお気に入りしているかどうかでフィルタ(ユーザー名, 1人分だけ)
     * @param limit 1度に表示する表示数
     * @param offset 作成済み記事N個のうち、Offset個分だけskipしてそこから表示(0の場合、0個分skip)
     * @return エラー or 作成済み記事一覧
     */
    fun execute(
        tag: String?,
        author: String?,
        favoritedByUsername: String?,
        limit: String?,
        offset: String?,
        currentUser: Option<RegisteredUser>
    ): Either<Error, CreatedArticlesWithMetadata> = TODO()

    /**
     * - フィルタ用パラメータのバリデーションエラー
     * - Offset値がフィルタ済み作成済み記事の数を超過エラー
     */
    sealed interface Error : MyError {
        data class FilterParametersValidationErrors(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class OffsetOverCreatedArticlesCountError(
            val filterParameters: FilterParameters,
            val articlesCount: Int,
            override val cause: MyError,
        ) : Error, MyError.MyErrorWithMyError
        // TODO: いらないかも:要検討
        data class NotFoundUser(
            val user: RegisteredUser,
            override val cause: MyError
        ) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class FilterCreatedArticleUseCaseImpl(val articleRepository: ArticleRepository) : FilterCreatedArticleUseCase
