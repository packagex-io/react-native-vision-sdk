package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Status(
    @SerializedName("outstanding_for_60_days")
    val outstandingFor60Days: Any?
)