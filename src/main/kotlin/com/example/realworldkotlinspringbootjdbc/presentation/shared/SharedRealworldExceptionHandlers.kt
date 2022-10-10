package com.example.realworldkotlinspringbootjdbc.presentation.shared

import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModel
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModelErrors
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 共通の例外ハンドラー
 *
 * 扱う項目(AOP系)
 * - 認証失敗
 * - セッションEncodeのエラー
 * - 技術的(異常系)例外
 *
 * 保守がしづらくなるので、基本的に増えないことが望ましい
 * ファイルも(あえて)分けない
 *   - 仮に分けるとしたら専用のパッケージを切る
 *
 * 命名ルール
 * on${例外名}
 */
@RestControllerAdvice
class SharedRealworldExceptionHandlers {
    /**
     * セッションのエンコード時に失敗した時のエラーハンドリング
     *
     * @return 基本的に500
     */
    @ExceptionHandler(value = [RealworldSessionEncodeErrorException::class])
    fun onRealworldSessionEncodeErrorException(): ResponseEntity<GenericErrorModel> =
        ResponseEntity(
            GenericErrorModel(GenericErrorModelErrors(body = listOf("想定外のエラーが起きました"))),
            HttpStatus.valueOf(500)
        )
}
