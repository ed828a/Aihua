package com.dew.aihua.util

class InvalidJsonException : Exception {
    private constructor() : super()

    constructor(message: String) : super(message)

    constructor(cause: Throwable) : super(cause)
}