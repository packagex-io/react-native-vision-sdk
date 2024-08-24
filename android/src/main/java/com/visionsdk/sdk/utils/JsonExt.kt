package io.packagex.visionsdk.utils

import org.json.JSONArray
import org.json.JSONObject


fun JSONObject.toSafeString(): String {
    val sb = StringBuilder()
    sb.append("{")

    val keys = this.keys()
    var first = true

    while (keys.hasNext()) {
        val key = keys.next()
        val value = this.opt(key)  // Use opt to safely handle null values

        if (!first) {
            sb.append(",")
        }

        sb.append("\"").append(escapeJson(key)).append("\"").append(":")
        when (value) {
            is JSONObject -> sb.append(value.toSafeString()) // Recursively convert nested JSONObject
            is JSONArray -> sb.append(value.toSafeString()) // Convert JSONArray
            is String -> sb.append("\"").append(escapeJson(value)).append("\"")
            is Number, is Boolean -> sb.append(value.toString())
            else -> sb.append("null")
        }

        first = false
    }

    sb.append("}")
    return sb.toString()
}

fun JSONArray.toSafeString(): String {
    val sb = StringBuilder()
    sb.append("[")

    for (i in 0 until this.length()) {
        if (i > 0) {
            sb.append(",")
        }

        val value = this.opt(i)  // Use opt to safely handle null values
        when (value) {
            is JSONObject -> sb.append(value.toSafeString()) // Recursively convert nested JSONObject
            is JSONArray -> sb.append(value.toSafeString()) // Recursively convert nested JSONArray
            is String -> sb.append("\"").append(escapeJson(value)).append("\"")
            is Number, is Boolean -> sb.append(value.toString())
            else -> sb.append("null")
        }
    }

    sb.append("]")
    return sb.toString()
}

private fun escapeJson(str: String): String {
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\b", "\\b")
        .replace("\u000c", "\\f")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}