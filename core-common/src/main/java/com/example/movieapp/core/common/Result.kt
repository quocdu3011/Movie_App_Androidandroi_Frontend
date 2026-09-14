package com.example.movieapp.core.common

sealed interface Result<out T> {
    data class Success<out T>(val data: T) : Result<T>
    data class Error(
        val code: String,
        val message: String,
        val requestId: String? = null,
        val throwable: Throwable? = null
    ) : Result<Nothing>
}

inline fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    is Result.Error -> null
}

inline fun <T> Result<T>.getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
    is Result.Success -> data
    is Result.Error -> defaultValue
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (Result.Error) -> Unit): Result<T> {
    if (this is Result.Error) action(this)
    return this
}
