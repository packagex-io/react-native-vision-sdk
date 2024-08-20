package io.packagex.visionsdk.modelclasses.ocr_response


import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("output") val output: Output?,
    @SerializedName("rawText") val rawText: String?,
    @SerializedName("status") val status: Int?,
    @SerializedName("uuid") val uuid: String?,
    @SerializedName("duplicateUuidValidation") val duplicate_uuid_validation: DuplicateUuidValidation,

    )