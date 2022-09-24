package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.presentation.response.Tags
import com.example.realworldkotlinspringbootjdbc.presentation.response.serializeUnexpectedErrorForResponseBody
import com.example.realworldkotlinspringbootjdbc.usecase.ListTagUseCase
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class DefaultController(
    val listTagUseCase: ListTagUseCase
) {
    /**
     * タグ一覧
     *
     * ```
     * curl -X GET --header 'Content-Type: application/json' 'http://localhost:8080/api/tags' | jq '.'
     * ```
     */
    @GetMapping("/tags")
    fun list(): ResponseEntity<String> {
        return when (val result = listTagUseCase.execute()) {
            /**
             * 失敗
             */
            is Left -> when (result.value) {
                /**
                 * 原因: 不明
                 */
                is ListTagUseCase.Error.Unexpected -> ResponseEntity(
                    serializeUnexpectedErrorForResponseBody("原因不明のエラーが発生しました"), // TODO: serializeUnexpectedErrorForResponseBodyをやめる
                    HttpStatus.valueOf(500)
                )
            }
            /**
             * 成功
             */
            is Right -> ResponseEntity(
                ObjectMapper().writeValueAsString(Tags(result.value.map { it.value })),
                HttpStatus.valueOf(200),
            )
        }
    }
}
