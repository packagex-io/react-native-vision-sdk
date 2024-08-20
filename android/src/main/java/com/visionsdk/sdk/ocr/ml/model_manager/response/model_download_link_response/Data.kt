package io.packagex.visionsdk.ocr.ml.model_manager.response.model_download_link_response


import com.google.gson.annotations.SerializedName

internal data class Data(
    @SerializedName("url")
    val url: String?
)