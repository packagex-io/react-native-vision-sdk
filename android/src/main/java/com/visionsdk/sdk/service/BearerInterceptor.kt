package io.packagex.visionsdk.service

import io.packagex.visionsdk.Authentication
import io.packagex.visionsdk.VisionSDK
import okhttp3.Interceptor
import okhttp3.Response

internal class BearerInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val result = when (val auth = VisionSDK.getInstance().auth) {
            is Authentication.API -> chain.request()
            is Authentication.BearerToken -> chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${auth.token}").build()

            null -> chain.request()
        }
        return chain.proceed(result)
    }
}