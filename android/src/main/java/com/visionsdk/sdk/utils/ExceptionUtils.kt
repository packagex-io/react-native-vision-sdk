package io.packagex.visionsdk.utils

fun printStackTrace() {
    try { throw Exception() } catch (e: Exception) { e.printStackTrace() }
}