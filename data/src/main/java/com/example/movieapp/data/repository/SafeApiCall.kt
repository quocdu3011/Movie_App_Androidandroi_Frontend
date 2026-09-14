package com.example.movieapp.data.repository

import com.example.movieapp.core.common.Result
import com.example.movieapp.core.network.model.ApiResponseDto
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response

suspend inline fun <T, R> safeApiCall(
    json: Json,
    crossinline apiCall: suspend () -> ApiResponseDto<T>,
    crossinline transform: (T) -> R
): Result<R> {
    return try {
        val response = apiCall()
        if (response.success) {
            if (response.data != null) {
                Result.Success(transform(response.data!!))
            } else {
                @Suppress("UNCHECKED_CAST")
                Result.Success(transform(null as T))
            }
        } else {
            val code = response.error?.code ?: "UNKNOWN_ERROR"
            val message = response.error?.message ?: "Unknown error occurred"
            Result.Error(code = code, message = message, requestId = response.requestId)
        }
    } catch (e: HttpException) {
        val errorResponseBody = e.response()?.errorBody()?.string()
        var code = "HTTP_${e.code()}"
        var message = e.message()
        var requestId: String? = null
        if (!errorResponseBody.isNullOrBlank()) {
            try {
                val errorDto = json.decodeFromString<ApiResponseDto<Unit>>(errorResponseBody)
                code = errorDto.error?.code ?: code
                message = errorDto.error?.message ?: message
                requestId = errorDto.requestId
            } catch (_: Exception) {}
        }
        Result.Error(code = code, message = message, requestId = requestId, throwable = e)
    } catch (e: Exception) {
        Result.Error(code = "CLIENT_ERROR", message = e.localizedMessage ?: "Network error", throwable = e)
    }
}

suspend inline fun safeVoidApiCall(
    json: Json,
    crossinline apiCall: suspend () -> Response<Unit>
): Result<Unit> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            Result.Success(Unit)
        } else {
            val errorResponseBody = response.errorBody()?.string()
            var code = "HTTP_${response.code()}"
            var message = response.message()
            var requestId: String? = null
            if (!errorResponseBody.isNullOrBlank()) {
                try {
                    val errorDto = json.decodeFromString<ApiResponseDto<Unit>>(errorResponseBody)
                    code = errorDto.error?.code ?: code
                    message = errorDto.error?.message ?: message
                    requestId = errorDto.requestId
                } catch (_: Exception) {}
            }
            Result.Error(code = code, message = message, requestId = requestId)
        }
    } catch (e: Exception) {
        Result.Error(code = "CLIENT_ERROR", message = e.localizedMessage ?: "Network error", throwable = e)
    }
}
