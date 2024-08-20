package io.packagex.visionsdk.modelclasses.ocr_response

data class Miscellaneous(
    val confidential_flag: Boolean,
    val food_delivey_flag: Boolean,
    val fragile_flag: Boolean,
    val legal_document_flag: Boolean,
    val oversize_flag: Boolean,
    val pay_stubs_flag: Boolean,
    val return_to_sender_flag: Boolean,
    val time_sensitive_flag: Boolean
)