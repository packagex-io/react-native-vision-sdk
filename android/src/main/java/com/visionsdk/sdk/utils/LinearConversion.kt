package io.packagex.visionsdk.utils

internal class LinearConversion<T : Number>(
    private val oldMin: T,
    private val oldMax: T,
    private val newMin: T,
    private val newMax: T
) {
    fun getValueAgainst(oldValue: T): T {

        return when (oldMax) {
            is Int -> {
                val oldRange = oldMax.toInt() - oldMin.toInt()
                val newRange = newMax.toInt() - newMin.toInt()
                ((((oldValue.toInt() - oldMin.toInt()) * newRange) / oldRange) + newMin.toInt()) as T
            }

            is Float -> {
                val oldRange = oldMax.toFloat() - oldMin.toFloat()
                val newRange = newMax.toFloat() - newMin.toFloat()
                ((((oldValue.toFloat() - oldMin.toFloat()) * newRange) / oldRange) + newMin.toFloat()) as T
            }

            is Double -> {
                val oldRange = oldMax.toDouble() - oldMin.toDouble()
                val newRange = newMax.toDouble() - newMin.toDouble()
                ((((oldValue.toDouble() - oldMin.toDouble()) * newRange) / oldRange) + newMin.toDouble()) as T
            }

            is Long -> {
                val oldRange = oldMax.toLong() - oldMin.toLong()
                val newRange = newMax.toLong() - newMin.toLong()
                ((((oldValue.toLong() - oldMin.toLong()) * newRange) / oldRange) + newMin.toLong()) as T
            }

            is Short -> {
                val oldRange = oldMax.toShort() - oldMin.toShort()
                val newRange = newMax.toShort() - newMin.toShort()
                ((((oldValue.toShort() - oldMin.toShort()) * newRange) / oldRange) + newMin.toShort()) as T
            }

            else -> {
                throw IllegalArgumentException("Given type is not supported")
            }
        }
    }
}