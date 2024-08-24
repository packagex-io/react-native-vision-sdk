package io.packagex.visionsdk.utils

import com.asadullah.handyutils.toLettersOrDigits
import java.text.Normalizer

fun String.removeSpecialCharacters(specialCharacterSet: String = "!()[]{};:'\",<>.?@#$%^&*_~"): String {
    val specialCharacters = specialCharacterSet.toSet()
    val formattedText = filter { it !in specialCharacters }
    return formattedText.trim()
}

private val REGEX_REMOVE_ACCENT = "\\p{InCombiningDiacriticalMarks}+".toRegex()

fun CharSequence.removeAccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return REGEX_REMOVE_ACCENT.replace(temp, "")
}

fun String.lowercaseAndClean(): String {
    return this.lowercase().toLettersOrDigits()!!
}

fun String.removeDuplicateCharacters(): String {
    return this.toList().distinct().joinToString("")
}