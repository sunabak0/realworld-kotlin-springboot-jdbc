package com.example.realworldkotlinspringbootjdbc.usecase.shared

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.util.MyError
import com.example.realworldkotlinspringbootjdbc.util.MySessionJwt
import org.springframework.stereotype.Component

/**
 * セッションJWT認証
 */
interface RealworldAuthenticationUseCase {
    /**
     * 実行
     *
     * - セッションJWTに登録済みユーザーIDとEmailが埋まっている
     * - セッション情報のEmail情報が、DBにある情報と異なる場合はJWTをデコードできてもセッションが古いとみなし、エラーを返す
     *
     * @param token JWT文字列
     * @return エラー or 登録済みユーザー
     */
    fun execute(token: String): Either<Unauthorized, RegisteredUser> = throw NotImplementedError()

    sealed interface Unauthorized {
        /**
         * Decodeに失敗
         */
        data class FailedDecodeToken(override val cause: MyError, val token: String) :
            Unauthorized,
            MyError.MyErrorWithMyError

        /**
         * 検索したUserが存在しなかった
         */
        data class NotFound(override val cause: MyError, val id: UserId) : Unauthorized, MyError.MyErrorWithMyError

        /**
         * Emailが合わなかった
         */
        data class NotMatchEmail(val oldEmail: Email, val newEmail: Email) : Unauthorized, MyError.Basic
    }
}

@Component
class RealworldAuthenticationUseCaseImpl(
    val userRepository: UserRepository,
    val mySessionJwt: MySessionJwt
) : RealworldAuthenticationUseCase {
    override fun execute(token: String): Either<RealworldAuthenticationUseCase.Unauthorized, RegisteredUser> {
        /**
         * JWTをセッション情報へデコード
         * Error -> 早期リターン
         */
        val decodedSession = mySessionJwt.decode(token).fold(
            { return RealworldAuthenticationUseCase.Unauthorized.FailedDecodeToken(it, token).left() },
            { it }
        )

        /**
         * セッション情報のUserIdから登録済みユーザーを取得
         * Error -> 早期リターン
         */
        val registeredUser = userRepository.findByUserId(decodedSession.userId).fold(
            {
                return when (it) {
                    is UserRepository.FindByUserIdError.NotFound -> RealworldAuthenticationUseCase.Unauthorized.NotFound(
                        it,
                        decodedSession.userId
                    ).left()
                }
            },
            { it }
        )

        /**
         * セッション情報のEmailと現在のEmailを比較
         * 不一致 -> エラー
         * 一致 -> 登録済みユーザー
         */
        return if (decodedSession.email.value == registeredUser.email.value) {
            registeredUser.right()
        } else {
            RealworldAuthenticationUseCase.Unauthorized.NotMatchEmail(decodedSession.email, registeredUser.email).left()
        }
    }
}
