package io.packagex.visionsdk.exceptions

import com.google.gson.Gson
import io.packagex.visionsdk.modelclasses.ErrorModel
import retrofit2.HttpException

class APIErrorResponse(httpException: HttpException) : Exception() {
    val errorModel: ErrorModel = try {
        Gson().fromJson(httpException.response()?.errorBody()?.string(), ErrorModel::class.java)
    } catch (e: Exception) {
        ErrorModel(
            code = null,
            endpoint = null,
            errors = null,
            message = e.message,
            pagination = null,
            status = null
        )
    }
}