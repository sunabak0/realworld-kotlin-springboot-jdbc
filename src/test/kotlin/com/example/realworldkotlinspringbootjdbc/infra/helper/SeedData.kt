package com.example.realworldkotlinspringbootjdbc.infra.helper

import arrow.core.toOption
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.OtherUser
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Body
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import java.util.Date
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body as CommentBody

/**
 * Infra層のTest実行前にSeedDataが入る
 * そのSeedDataの全てをDomainObjectで事前に表現
 *
 * SeedDataを用意している理由
 * - DBに入れるSeedレコードの変更に気づくため(そのため、テストも記述する)
 * - 既に出来上がってるモデルがあると、Domain層やInfra層のテストの時にも便利
 */
object SeedData {
    /**
     * @return DBにSeedDataとして入っているRegisteredUser郡
     */
    fun users(): Set<RegisteredUser> = setOf(
        RegisteredUser.newWithoutValidation(
            UserId(1),
            email = Email.newWithoutValidation("paul-graham@example.com"),
            username = Username.newWithoutValidation("paul-graham"),
            bio = Bio.newWithoutValidation("Lisper"),
            image = Image.newWithoutValidation(""),
        ),
        RegisteredUser.newWithoutValidation(
            UserId(2),
            email = Email.newWithoutValidation("matz@example.com"),
            username = Username.newWithoutValidation("松本行弘"),
            bio = Bio.newWithoutValidation("Rubyを作った"),
            image = Image.newWithoutValidation(""),
        ),
        RegisteredUser.newWithoutValidation(
            UserId(3),
            email = Email.newWithoutValidation("graydon-hoare@example.com"),
            username = Username.newWithoutValidation("graydon-hoare"),
            bio = Bio.newWithoutValidation("Rustを作った"),
            image = Image.newWithoutValidation(""),
        ),
    )

    /**
     * @return DBにSeedDataとして入っているタグ郡
     */
    fun tags(): Set<Tag> = setOf(
        Tag.newWithoutValidation("rust"),
        Tag.newWithoutValidation("scala"),
        Tag.newWithoutValidation("kotlin"),
        Tag.newWithoutValidation("ocaml"),
        Tag.newWithoutValidation("elixir"),
    )

    /**
     * @return DBにSeedDataとして入っている作成済み記事郡
     */
    fun createdArticles(): Set<CreatedArticle> = setOf(
        generateCreatedArticleWithFavorited(ArticleId(1), false),
        generateCreatedArticleWithFavorited(ArticleId(2), false),
        generateCreatedArticleWithFavorited(ArticleId(3), false),
    )

    /**
     * @return DBにSeedDataとして入っている特定のユーザー視点から見た時のお気に入りの作成済み記事郡(ユーザー全員分)
     */
    fun createdArticlesFromViewpointSet(): Map<UserId, Set<CreatedArticle>> = mapOf(
        UserId(1) to setOf(
            generateCreatedArticleWithFavorited(ArticleId(1), false),
            generateCreatedArticleWithFavorited(ArticleId(2), false),
            generateCreatedArticleWithFavorited(ArticleId(3), true),
        ),
        UserId(2) to setOf(
            generateCreatedArticleWithFavorited(ArticleId(1), true),
            generateCreatedArticleWithFavorited(ArticleId(2), false),
            generateCreatedArticleWithFavorited(ArticleId(3), true),
        ),
        UserId(3) to setOf(
            generateCreatedArticleWithFavorited(ArticleId(1), false),
            generateCreatedArticleWithFavorited(ArticleId(2), true),
            generateCreatedArticleWithFavorited(ArticleId(3), false),
        ),
    )

    /**
     * SeedDataである作成済み記事のDomainObjectジェネレータ
     *
     * - お気に入り済みかどうかを任意で入れられる
     *
     * @param articleId
     * @param favorited
     * @return ジェネレートされた作成済み記事
     */
    private fun generateCreatedArticleWithFavorited(articleId: ArticleId, favorited: Boolean): CreatedArticle =
        when (articleId) {
            ArticleId(1) -> CreatedArticle.newWithoutValidation(
                id = articleId,
                title = Title.newWithoutValidation("Rust vs Scala vs Kotlin"),
                slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                body = Body.newWithoutValidation("dummy-body"),
                createdAt = Date(),
                updatedAt = Date(),
                description = Description.newWithoutValidation("dummy-description"),
                tagList = listOf(
                    Tag.newWithoutValidation("rust"),
                    Tag.newWithoutValidation("scala"),
                    Tag.newWithoutValidation("kotlin"),
                ),
                authorId = UserId(1),
                favorited = favorited,
                favoritesCount = 1,
            )

            ArticleId(2) -> CreatedArticle.newWithoutValidation(
                id = articleId,
                title = Title.newWithoutValidation("Functional programming kotlin"),
                slug = Slug.newWithoutValidation("functional-programming-kotlin"),
                body = Body.newWithoutValidation("dummy-body"),
                createdAt = Date(),
                updatedAt = Date(),
                description = Description.newWithoutValidation("dummy-description"),
                tagList = listOf(
                    Tag.newWithoutValidation("kotlin"),
                ),
                authorId = UserId(1),
                favorited = favorited,
                favoritesCount = 1,
            )

            ArticleId(3) -> CreatedArticle.newWithoutValidation(
                id = articleId,
                title = Title.newWithoutValidation("TDD(Type Driven Development)"),
                slug = Slug.newWithoutValidation("tdd-type-driven-development"),
                body = Body.newWithoutValidation("dummy-body"),
                createdAt = Date(),
                updatedAt = Date(),
                description = Description.newWithoutValidation("dummy-description"),
                tagList = emptyList(),
                authorId = UserId(2),
                favorited = favorited,
                favoritesCount = 2,
            )

            else -> throw IllegalArgumentException("articleId(value=$articleId)の定義がありません")
        }

    /**
     * @return DBにSeedDataとして入っている作成済み記事に紐付いているコメント一覧
     */
    fun comments(): Map<ArticleId, Set<Comment>> = mapOf(
        ArticleId(1) to setOf(
            Comment.newWithoutValidation(
                id = CommentId.newWithoutValidation(1),
                body = CommentBody.newWithoutValidation("dummy-comment-body-01"),
                createdAt = Date(),
                updatedAt = Date(),
                authorId = UserId(3),
            ),
            Comment.newWithoutValidation(
                id = CommentId.newWithoutValidation(3),
                body = CommentBody.newWithoutValidation("dummy-comment-body-03"),
                createdAt = Date(),
                updatedAt = Date(),
                authorId = UserId(2),
            ),
            Comment.newWithoutValidation(
                id = CommentId.newWithoutValidation(5),
                body = CommentBody.newWithoutValidation("dummy-comment-body-02"),
                createdAt = Date(),
                updatedAt = Date(),
                authorId = UserId(3),
            ),
        ),
        ArticleId(2) to emptySet(),
        ArticleId(3) to setOf(
            Comment.newWithoutValidation(
                id = CommentId.newWithoutValidation(2),
                body = CommentBody.newWithoutValidation("dummy-comment-body-02"),
                createdAt = Date(),
                updatedAt = Date(),
                authorId = UserId(1),
            ),
            Comment.newWithoutValidation(
                id = CommentId.newWithoutValidation(4),
                body = CommentBody.newWithoutValidation("dummy-comment-body-04"),
                createdAt = Date(),
                updatedAt = Date(),
                authorId = UserId(3),
            ),
        )
    )

    /**
     * @return ログインしていない人から見た他ユーザー郡
     */
    fun otherUsers(): Set<OtherUser> = users().map { generateOtherUserWithFollowing(it.userId, false) }.toSet()

    /**
     * @return 対象のユーザーから見た他ユーザー郡
     */
    fun otherUsersFromViewpointSet(): Map<UserId, Set<OtherUser>> = mapOf(
        UserId(1) to setOf(
            generateOtherUserWithFollowing(UserId(1), false),
            generateOtherUserWithFollowing(UserId(2), false),
            generateOtherUserWithFollowing(UserId(3), false),
        ),
        UserId(2) to setOf(
            generateOtherUserWithFollowing(UserId(1), true),
            generateOtherUserWithFollowing(UserId(2), false),
            generateOtherUserWithFollowing(UserId(3), false),
        ),
        UserId(3) to setOf(
            generateOtherUserWithFollowing(UserId(1), true),
            generateOtherUserWithFollowing(UserId(2), true),
            generateOtherUserWithFollowing(UserId(3), false),
        ),
    )

    /**
     * SeedDataである他ユーザーのDomainObjectジェネレータ
     *
     * - フォロー済みかどうかを任意で入れられる
     *
     * @param userId
     * @param following
     * @return ジェネレートされた他ユーザー
     */
    private fun generateOtherUserWithFollowing(userId: UserId, following: Boolean): OtherUser =
        users().find { it.userId == userId }.toOption().fold(
            { throw IllegalArgumentException("UserId(value=$userId)のユーザがいません") },
            {
                OtherUser.newWithoutValidation(
                    userId = it.userId,
                    username = it.username,
                    bio = it.bio,
                    image = it.image,
                    following = following,
                )
            }
        )
}
