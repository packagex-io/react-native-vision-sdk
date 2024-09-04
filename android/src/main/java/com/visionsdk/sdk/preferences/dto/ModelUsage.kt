package io.packagex.visionsdk.preferences.dto

import com.google.gson.annotations.SerializedName
import io.packagex.visionsdk.ocr.ml.core.ModelClass
import io.packagex.visionsdk.ocr.ml.core.ModelSize

data class ModelUsage(

    @SerializedName("model_class")
    val modelClass: ModelClass,

    @SerializedName("model_size")
    val modelSize: ModelSize,

    @SerializedName("usage_count")
    val usageCount: Int,

    @SerializedName("usage_duration")
    val usageDuration: Long
)