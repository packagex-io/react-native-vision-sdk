package io.packagex.visionsdk.service.all

/*
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
}*/
