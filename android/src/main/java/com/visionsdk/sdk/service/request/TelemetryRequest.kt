package io.packagex.visionsdk.service.request

import com.google.gson.annotations.SerializedName
import java.util.UUID

internal data class TelemetryRequest(

    @SerializedName("_d")
    val deviceId: String,

    @SerializedName("_i")
    val sdkId: String,

    @SerializedName("_f")
    val framework: String,

    @SerializedName("_t")
    val telemetryDataList: List<TelemetryData>
) {
    @SerializedName("_p")
    val platform = "android"
}

internal data class TelemetryData(

    @SerializedName("_i")
    val id: String = UUID.randomUUID().toString(),

    @SerializedName("_a")
    val action: String,

    @SerializedName("_u")
    val actionPerformedAt: String,

    @SerializedName("_m")
    val modelInfo: ModelInfo? = null,

    @SerializedName("_p1")
    val extractionTimeInMillis: Long,

    @SerializedName("_p2")
    val matchingTimeInMillis: Long? = null,

    @SerializedName("_p3")
    val phase3TimeInMillis: Long? = null,

    @SerializedName("_p4")
    val phase4TimeInMillis: Long? = null,

    @SerializedName("_t")
    val totalTimeInMillis: Long? = null,

    @SerializedName("_c_min")
    val minCpu: Long? = null,

    @SerializedName("_c_avg")
    val avgCpu: Long? = null,

    @SerializedName("_c_std")
    val stdDevCpu: Long? = null,

    @SerializedName("_c_max")
    val maxCpu: Long? = null,

    @SerializedName("_rt")
    val report: String? = null,

    @SerializedName("_et")
    val error: String? = null,

    @SerializedName("_o_1")
    val object1: Map<String, Any?>? = null,

    @SerializedName("_o_2")
    val object2: Map<String, Any?>? = null,

    @SerializedName("_o_3")
    val object3: Map<String, Any?>? = null,

    @SerializedName("_o_4")
    val object4: Map<String, Any?>? = null,

    @SerializedName("_b")
    val base64ImageToReportOn: String? = null
)

internal data class ModelInfo(

    @SerializedName("_i")
    val modelId: String,

    @SerializedName("_mv")
    val modelVersion: ModelVersion,
)

internal data class ModelVersion(@SerializedName("_i") val modelVersionId: String)