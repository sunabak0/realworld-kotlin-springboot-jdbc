package com.example.realworldkotlinspringbootjdbc.domain.article

interface Description {
    val value: String

    /**
     * 実装
     */
    private data class DescriptionWithoutValidation(override val value: String) : Description

    /**
     * Factory メソッド
     */
    companion object {
        /**
         * Validation 無し
         */
        fun newWithoutValidation(description: String): Description = DescriptionWithoutValidation(description)
    }
}