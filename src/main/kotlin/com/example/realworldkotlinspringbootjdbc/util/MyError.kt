package com.example.realworldkotlinspringbootjdbc.util

interface MyError {
    interface ValidationError : MyError {
        val message: String
    }
    interface MyErrorWithThrowable : MyError { val cause: Throwable }

    interface MyErrorWithMyError : MyError { val cause: MyError }

    interface MyErrors : MyError { val errors: List<MyError> }
    interface ValidationErrors<E : ValidationError> : MyError { val errors: List<E> }
}
