package io.packagex.visionsdk.modelclasses.ocr_response


import com.google.gson.annotations.SerializedName

data class Address(
    @SerializedName("receiverAddress")
    val receiverAddress: ReceiverAddress?,
    @SerializedName("senderAddress")
    val senderAddress: SenderAddress?
)