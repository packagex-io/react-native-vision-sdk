package io.packagex.visionsdk.service.request

internal data class ConnectRequest(
    val _i: String,
    val _d: String,
    val _f: String,
    val _m: ConnectModelRequest? = null,
    val _uc: Int? = null,
    val _tc: Long? = null
) {
    val _p = "android"
}

internal data class ConnectModelRequest(
    val _t: String,
    val _s: String? = null,
    val _d: Boolean? = null
) {
    val _c = "image"
    val _l = "Latn"
}