package io.packagex.visionsdk.service

import io.packagex.visionsdk.BuildConfig
import io.packagex.visionsdk.Environment
import io.packagex.visionsdk.VisionSDK
import io.packagex.visionsdk.service.manifest.ManifestApiServiceKey
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Duration

internal object ServiceBuilder {

    private val client by lazy {
        OkHttpClient
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
    }

    // AI: Note that we have two Retrofit objects. That is because we have two base urls to cater for.

    private val retrofit by lazy {
        Retrofit
            .Builder()
            .baseUrl(getUrl(VisionSDK.getInstance().environment))
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                client
                    .newBuilder()
                    .build()
            )
            .build()
    }

    private val manifestRetrofit by lazy {
        Retrofit
            .Builder()
            .baseUrl(getManifestUrl(VisionSDK.getInstance().environment))
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                client
                    .newBuilder()
                    .build()
            )
            .build()
    }

    fun <T> buildService(service: Class<T>): T {
        return if (service::class == ManifestApiServiceKey::class) {
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