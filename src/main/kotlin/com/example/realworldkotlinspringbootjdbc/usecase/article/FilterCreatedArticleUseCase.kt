package com.example.realworldkotlinspringbootjdbc.usecase.article

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Invalid
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.Valid
import arrow.core.left
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
class FilterCreatedArticleUseCaseImpl(val articleRepository: ArticleRepository) : FilterCreatedArticleUseCase {
    override fun execute(
        tag: String?,
        author: String?,
        favoritedByUsername: String?,
        limit: String?,
        offset: String?,
        currentUser: Option<RegisteredUser>
    ): Either<FilterCreatedArticleUseCase.Error, CreatedArticlesWithMetadata> =
        when (val filterParameters = FilterParameters.new(tag, author, favoritedByUsername, limit, offset)) {
            /**
             * フィルタ用パラメータ: バリデーションエラー
             */
            is Invalid -> FilterCreatedArticleUseCase.Error.FilterParametersValidationErrors(
                filterParameters.value
            ).left()
            /**
             * フィルタ用パラメータ: 作成成功
             */
            is Valid -> when (currentUser) {
                /**
                 * 作成済み記事一覧取得
                 */
                None -> when (val filterResult = articleRepository.filter(filterParameters.value)) {
                    /**
                     * 取得失敗
                     */
                    is Left -> when (val error = filterResult.value) {
                        /**
                         * 原因: フィルタ用パラメータの offset が 作成済み記事一覧の数 を超えている
                         * 何をどう表示するかはPresentation層に任せる
                         */
                        is ArticleRepository.FilterError.OffsetOverTheNumberOfFilteredCreatedArticlesError ->
                            FilterCreatedArticleUseCase.Error.OffsetOverCreatedArticlesCountError(
                                filterParameters = filterParameters.value,
                                articlesCount = error.createdArticleCount,
                                cause = error
                            ).left()
                    }
                    /**
                     * 取得成功
                     */
                    is Right -> filterResult
                }
                /**
                 * 特定の登録済みユーザー から見た 作成済み記事一覧取得
                 */
                is Some -> when (val filterResult = articleRepository.filterFromRegisteredUserViewpoint(filterParameters.value, currentUser.value.userId)) {
                    /**
                     * 取得失敗
                     */
                    is Left -> when (val error = filterResult.value) {
                        /**
                         * 原因: ユーザーが見つからなかった
                         */
                        is ArticleRepository.FilterFromRegisteredUserViewpointError.NotFoundUser -> FilterCreatedArticleUseCase.Error.NotFoundUser(
                            user = currentUser.value,
                            cause = error
                        ).left()
                        /**
                         * 原因: フィルタ用パラメータの offset が 作成済み記事一覧の数 を超えている
                         * 何をどう表示するかはPresentation層に任せる
                         */
                        is ArticleRepository.FilterFromRegisteredUserViewpointError.OffsetOverTheNumberOfFilteredCreatedArticlesError ->
                            FilterCreatedArticleUseCase.Error.OffsetOverCreatedArticlesCountError(
                                filterParameters = filterParameters.value,
                                articlesCount = error.createdArticleCount,
                                cause = error
                            ).left()
                    }
                    /**
                     * 取得成功
                     */
                    is Right -> filterResult
                }
            }
        }
}
