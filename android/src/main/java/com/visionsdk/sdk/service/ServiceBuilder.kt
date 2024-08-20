package io.packagex.visionsdk.service

import com.visionsdk.BuildConfig
import io.packagex.visionsdk.Environment
import io.packagex.visionsdk.VisionSDK
import io.packagex.visionsdk.service.manifest.ManifestApiService
import io.packagex.visionsdk.service.manifest.ManifestBearerInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Duration

internal object ServiceBuilder {

    private val client = OkHttpClient
        .Builder()
        .connectTimeout(Duration.ofSeconds(30))
        .readTimeout(Duration.ofSeconds(30))
        .writeTimeout(Duration.ofSeconds(30))
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
                )
            }
        }
        .build()

    // AI: Note that we have two Retrofit objects. That is because we have two base urls to cater for.

    private val retrofit = Retrofit
        .Builder()
        .baseUrl(getUrl(VisionSDK.getInstance().environment ?: throw RuntimeException("VisionSDK was not initialized")))
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            client
                .newBuilder()
                .addInterceptor(BearerInterceptor())
                .build()
        )
        .build()

    private val manifestRetrofit = Retrofit
        .Builder()
        .baseUrl(getManifestUrl(VisionSDK.getInstance().environment ?: throw RuntimeException("VisionSDK was not initialized")))
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            client
                .newBuilder()
                .addInterceptor(ManifestBearerInterceptor())
                .build()
        )
        .build()

    fun <T> buildService(service: Class<T>): T {
        return if (service::class == ManifestApiService::class) {
            manifestRetrofit.create(service)
        } else {
            retrofit.create(service)
        }
    }

    private fun getUrl(environment: Environment): String {
        return when (environment) {
            Environment.DEV        -> "https://dev--api.packagex.io/v1/"
            Environment.QA         -> "https://qa--api.packagex.io/v1/"
            Environment.STAGING    -> "https://staging--api.packagex.io/v1/"
            Environment.SANDBOX    -> "https://sandbox--api.packagex.io/v1/"
            Environment.PRODUCTION -> "https://api.packagex.io/v1/"
        }
    }

    private fun getManifestUrl(environment: Environment): String {
        return when (environment) {
            Environment.DEV        -> "https://v1.packagex.io/iex/api/"
            Environment.QA         -> "https://v1.packagex.io/iex/api/"
            Environment.STAGING    -> "https://v1.packagex.io/iex/api/"
            Environment.SANDBOX    -> "https://v1.packagex.io/iex/api/"
            Environment.PRODUCTION -> "https://v1.packagex.io/iex/api/"
        }
    }
}
