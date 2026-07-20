package com.persons.finder.vo

data class ApiResponse<T>(
    val success: Boolean,
    val code: Int? = null,
    val data: T? = null,
    val runningTime: Double = 0.0,
    val message: String? = null
) {
    companion object {
        fun <T> success(data: T, runningTime: Double = 0.0): ApiResponse<T> {
            return ApiResponse(success = true, code = 200, data = data, runningTime = runningTime)
        }

        fun <T> error(message: String, code: Int = 400): ApiResponse<T> {
            return ApiResponse(success = false, code = code, message = message)
        }
    }
}
