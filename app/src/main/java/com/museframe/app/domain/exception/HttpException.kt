package com.museframe.app.domain.exception

/**
 * Custom exception for HTTP errors with status code
 */
class HttpException(
    val code: Int,
    message: String? = null
) : Exception(message ?: "HTTP error $code") {

    fun is404(): Boolean = code == 404

    fun isNotFound(): Boolean = code == 404

    fun isUnauthorized(): Boolean = code == 401

    fun isServerError(): Boolean = code in 500..599
}