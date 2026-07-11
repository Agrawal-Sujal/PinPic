package com.sunflower.pinpic.data.remote

import android.util.Log
import com.sunflower.pinpic.common.Result
import com.sunflower.pinpic.data.remote.dto.ErrorResponse
import kotlinx.serialization.json.Json
import retrofit2.Response

suspend fun <T> parseResponse(
    apiCall: suspend () -> Response<T>
): Result<T> {
    return try {
        val response = apiCall()

        Log.d("TAG",response.toString())

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null || response.code() == 204) {
                // If it's a 204 No Content or Unit response, we can safely cast or return success
                @Suppress("UNCHECKED_CAST")
                Result.Success(body as T)
            } else {
                Result.Error("Empty response body")
            }
        } else {
            val errorBody = response.errorBody()?.string()
            Log.d("TAG",errorBody.toString())

            val errorMessage = try {
                if (errorBody != null) {
                    val errorResponse = Json.decodeFromString<ErrorResponse>(errorBody)
                    errorResponse.error
                } else {
                    "Unknown error"
                }
            } catch (_: Exception) {
                "Failed to parse error response"
            }
            Result.Error(errorMessage)
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error occurred")
    }
}
