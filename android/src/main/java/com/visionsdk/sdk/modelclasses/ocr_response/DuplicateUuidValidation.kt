package io.packagex.visionsdk.modelclasses.ocr_response

data class DuplicateUuidValidation(
    val duplicate_uuid_flag: Boolean,
    val new_uuid: String,
    val old_uuid: String
)