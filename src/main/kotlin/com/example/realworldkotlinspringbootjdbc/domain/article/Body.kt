package com.example.realworldkotlinspringbootjdbc.domain.article

interface Body {
    val value: String

    /**
     * 実装
     */
    private data class BodyWithoutValidation(override val value: String) : Body

    /**
     * Factory メソッド
     */
    companion object {
        /**
         * Validation 無し
         */
        fun newWithoutValidation(body: String): Body = BodyWithoutValidation(body)
    }
}
