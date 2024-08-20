package io.packagex.visionsdk.modelclasses.ocr_response


import com.google.gson.annotations.SerializedName

data class Output(
    @SerializedName("scanOutput")
    val scanOutput: ScanOutput?,
    @SerializedName("blurrValue")
    val blurrValue: Any,
    @SerializedName("duplicatePackageFlag")
    val duplicate_package_flag: Boolean,
    @SerializedName("duplicatePackages")
    val duplicatePackages: List<Any>,
    @SerializedName("platform")
    val platform: String,
    @SerializedName("timeLogs")
    val timeLogs: TimeLogs

)