package io.packagex.visionsdk.service.response


import com.google.gson.annotations.SerializedName

data class ConnectResponse(
    @SerializedName("code")
    val code: Any?,
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("endpoint")
    val endpoint: Any?,
    @SerializedName("errors")
    val errors: List<Any?>?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("pagination")
    val pagination: Any?,
    @SerializedName("status")
    val status: Int?
) {
    data class Data(
        @SerializedName("_af")
        val af: Af?,
        @SerializedName("_av")
        val av: List<Av?>?,
        @SerializedName("_b")
        val b: B?,
        @SerializedName("_c")
        val c: Int?,
        @SerializedName("_i")
        val i: Int?,
        @SerializedName("_o")
        val o: Int?,
        @SerializedName("_r")
        val r: Any?,
        @SerializedName("_rq")
        val rq: Rq?,
        @SerializedName("_s")
        val s: S?,
        @SerializedName("_t")
        val t: Int?,
        @SerializedName("_u")
        val u: String?
    ) {
        data class Af(
            @SerializedName("_pt")
            val pt: Boolean?
        )

        data class Av(
            @SerializedName("_c")
            val c: String?,
            @SerializedName("_d")
            val d: Any?,
            @SerializedName("_ds")
            val ds: String?,
            @SerializedName("_i")
            val i: String?,
            @SerializedName("_l")
            val l: String?,
            @SerializedName("_mv")
            val mv: Mv?,
            @SerializedName("_n")
            val n: String?,
            @SerializedName("_s")
            val s: String?,
            @SerializedName("_t")
            val t: String?
        ) {
            data class Mv(
                @SerializedName("_c")
                val c: Boolean?,
                @SerializedName("_d")
                val d: Boolean?,
                @SerializedName("_ds")
                val ds: String?,
                @SerializedName("_h")
                val h: String?,
                @SerializedName("_i")
                val i: String?,
                @SerializedName("_v")
                val v: String?
            )
        }

        data class B(
            @SerializedName("_c")
            val c: Boolean?,
            @SerializedName("_d")
            val d: Boolean?,
            @SerializedName("_e")
            val e: Boolean?,
            @SerializedName("_le")
            val le: String?,
            @SerializedName("_lr")
            val lr: Boolean?
        )

        data class Rq(
            @SerializedName("_c")
            val c: String?,
            @SerializedName("_d")
            val d: D?,
            @SerializedName("_ds")
            val ds: String?,
            @SerializedName("_i")
            val i: String?,
            @SerializedName("_l")
            val l: String?,
            @SerializedName("_mv")
            val mv: Mv?,
            @SerializedName("_n")
            val n: String?,
            @SerializedName("_s")
            val s: String?,
            @SerializedName("_t")
            val t: String?
        ) {
            data class D(
                @SerializedName("_k")
                val k: String?,
                @SerializedName("_u")
                val u: String?
            )

            data class Mv(
                @SerializedName("_c")
                val c: Boolean?,
                @SerializedName("_d")
                val d: Boolean?,
                @SerializedName("_ds")
                val ds: String?,
                @SerializedName("_h")
                val h: String?,
                @SerializedName("_i")
                val i: String?,
                @SerializedName("_v")
                val v: String?
            )
        }

        data class S(
            @SerializedName("_ds")
            val ds: String?,
            @SerializedName("_i")
            val i: String?,
            @SerializedName("_p")
            val p: String?,
            @SerializedName("_v")
            val v: String?
        )
    }
}