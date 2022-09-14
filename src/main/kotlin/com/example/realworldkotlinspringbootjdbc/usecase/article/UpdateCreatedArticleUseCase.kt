package com.example.realworldkotlinspringbootjdbc.usecase.article

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.ArticleRepository
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.usecase.shared_model.CreatedArticleWithAuthor
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.springframework.stereotype.Service

/**
 * 作成済み記事の更新
 *
 * - nullの場合はもともとのやつで更新する
 * - 変更する箇所がなくてもUpdateする(updated_atを更新)
 * - 著者じゃないと更新できない
 * - タグリストは更新できない(作成時そのまま)
 */
interface UpdateCreatedArticleUseCase {
    /**
     * 実行
     *
     * @param requestedUser リクエストしたユーザー(著者である必要がある)
     * @param slug Slug
     * @param title 更新したいタイトル(nullの場合はもとのやつが採用される)
     * @param description 更新したいdescription(同上)
     * @param body 更新したいbody(同上)
     * @return エラー or 著者情報付きの作成済み記事
     */
    fun execute(
        requestedUser: RegisteredUser,
        slug: String?,
        title: String?,
        description: String?,
        body: String?,
    ): Either<Error, CreatedArticleWithAuthor> = TODO()

    sealed interface Error : MyError {
        data class InvalidArticle(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class NotFoundArticle(val slug: Slug) : Error, MyError.Basic
        data class NotAuthor(
            override val cause: MyError,
            val targetArticle: CreatedArticle,
            val notAuthorizedUser: RegisteredUser,
        ) : Error, MyError.MyErrorWithMyError
    }
}

@Service
class UpdateCreatedArticleUseCaseImpl(
    val articleRepository: ArticleRepository,
)
