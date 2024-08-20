package io.packagex.visionsdk.modelclasses.ocr_response


import com.google.gson.annotations.SerializedName

data class DataX(
    @SerializedName("recipientFound")
    val recipientFound: List<RecipientFound?>?,
    @SerializedName("senderFound")
    val senderFound: List<SenderFound?>?,
    @SerializedName("businessesFound")
    val businessesFound: List<Any>,
    @SerializedName("membersFound")
    val membersFound: List<Any>,
    @SerializedName("nonMembersFound")
    val nonMembersFound: List<NonMembersFound>,
    @SerializedName("suggestions")
    val suggestions: List<Any>
)