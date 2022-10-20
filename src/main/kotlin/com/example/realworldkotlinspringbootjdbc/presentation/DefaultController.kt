package com.example.realworldkotlinspringbootjdbc.presentation

import com.example.realworldkotlinspringbootjdbc.openapi.generated.controller.DefaultApi
import com.example.realworldkotlinspringbootjdbc.openapi.generated.model.TagsResponse
import com.example.realworldkotlinspringbootjdbc.usecase.ListTagUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class DefaultController(
    val listTagUseCase: ListTagUseCase
) : DefaultApi {
    override fun tagsGet(): ResponseEntity<TagsResponse> =
        listTagUseCase.execute().fold(
            { throw UnsupportedOperationException("成功する想定なため、この分岐には入らない") },
            {
                ResponseEntity(
                    TagsResponse(tags = it.map { it.value }.toList()),
                    HttpStatus.valueOf(200),
                )
            }
        )
}
