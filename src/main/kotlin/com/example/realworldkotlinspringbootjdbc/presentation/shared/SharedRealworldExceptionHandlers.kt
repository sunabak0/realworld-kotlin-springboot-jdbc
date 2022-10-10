package com.example.realworldkotlinspringbootjdbc.presentation.shared

import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModel
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.GenericErrorModelErrors
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
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
     * JSONのパースやMappingに失敗した場合のエラーハンドリング
     *
     * - リクエストボディが{ "foo": "hello }や""等、JSONのパースに失敗する場合、ここでエラーハンドリングされる
     * - リクエストボディがrequire = true等のフィールドを満たしていない場合、ここでエラーハンドリングされる
     *   - 例: @field:JsonProperty("user", required = true) val user: String
     *
     * 参考
     * - [Additional Handling of Jackson Parsing and Mapping Errors](https://dougbreaux.github.io/2020/10/11/Additional-Jackson-Parse-Errors.html)
     */
    @ExceptionHandler(value = [HttpMessageNotReadableException::class])
    fun onBindException(e: HttpMessageNotReadableException): ResponseEntity<GenericErrorModel> =
        when (e.cause) {
            is JsonParseException -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("リクエストボディが読み取れませんでした(Jsonとして間違っている可能性があります)"))),
                HttpStatus.valueOf(400)
            )

            is MismatchedInputException -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("リクエストボディが読み取れませんでした(要求しているJSONの形ではない可能性があります)"))),
                HttpStatus.valueOf(400)
            )

            is JsonMappingException -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("リクエストボディが読み取れませんでした(マッピングに失敗した可能性があります)"))),
                HttpStatus.valueOf(400)
            )

            else -> ResponseEntity(
                GenericErrorModel(GenericErrorModelErrors(body = listOf("リクエストボディが読み取れませんでした"))),
                HttpStatus.valueOf(400)
            )
        }

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
