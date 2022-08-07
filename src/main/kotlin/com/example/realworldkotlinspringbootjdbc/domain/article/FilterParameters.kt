package com.example.realworldkotlinspringbootjdbc.domain.article

import arrow.core.Option
import arrow.core.ValidatedNel
import arrow.core.zip
import arrow.typeclasses.Semigroup
import com.example.realworldkotlinspringbootjdbc.domain.article.filter_parameters.Limit
import com.example.realworldkotlinspringbootjdbc.domain.article.filter_parameters.Offset
import com.example.realworldkotlinspringbootjdbc.util.MyError

/**
 * フィルタ用パラメーター
 *
 * 作成済み記事一覧のフィルタに使うパラメータ郡
 *
 * tag: タグでフィルタ
 * author: 書いた人でフィルタ
 * favoritesByUsername: 特定のユーザーがお気に入り済みかどうかでフィルタ
 * limit: 1度に表示する記事の最大値
 * offset: 何個目から記事を表示するか(100個あるうち、offset=10で11個目から等)
 *
 * 注
 * - Option<T>の時、Noneはフィルタなしを表現する
 * - Usernameなどを利用して、バリデーションは通さない
 *   - 例: InvalidなUsernameをNoneにした時、フィルタ無しになってしまうため
 */
interface FilterParameters {
    val tag: Option<String>
    val author: Option<String>
    val favoritedByUsername: Option<String>
    val limit: Limit
    val offset: Offset

    /**
     * 実装
     */
    private data class ValidatedFilterParameters(
        override val tag: Option<String>,
        override val author: Option<String>,
        override val favoritedByUsername: Option<String>,
        override val limit: Limit,
        override val offset: Offset
    ) : FilterParameters

    /**
     * Factory メソッド
     */
    companion object {
        /**
         * new
         *
         * - バリデーション有り
         *
         * @param tag
         * @param author
         * @param favoritedByUsername
         * @param limit
         * @param offset
         * @return バリデーションエラー or フィルタ用パラメーター
         */
        fun new(
            tag: String?,
            author: String?,
            favoritedByUsername: String?,
            limit: String?,
            offset: String?,
        ): ValidatedNel<MyError.ValidationError, FilterParameters> =
            Limit.new(limit).zip(
                Semigroup.nonEmptyList(),
                Offset.new(offset)
            ) { a, b ->
                ValidatedFilterParameters(
                    tag = Option.fromNullable(tag),
                    author = Option.fromNullable(author),
                    favoritedByUsername = Option.fromNullable(favoritedByUsername),
                    limit = a,
                    offset = b
                )
            }
    }
}
