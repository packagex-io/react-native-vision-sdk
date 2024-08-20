package io.packagex.visionsdk.ocr.regex

import com.google.code.regexp.Pattern

internal data class VisionRegex(val regexString: String, val regexType: RegexType) {

    private val pattern = Pattern.compile(regexString)

    /**
     * Attempts to find the next subsequence of the input sequence that matches the pattern.
     * This method starts at the beginning of this matcher's region, or, if a previous invocation of the method was successful and the matcher has not since been reset, at the first character not matched by the previous match.
     * If the match succeeds then more information can be obtained via the start, end, and group methods.
     * @param value CharSequence to check against given regex pattern
     * @return true if, and only if, a subsequence of the input sequence matches this matcher's pattern
     */
    fun find(value: CharSequence?): Boolean {
        return value?.let {
            val matcher = pattern.matcher(it)
            matcher.find()
        } ?: false
    }

    /**
     * Attempts to match the entire region against the pattern.
     * If the match succeeds then more information can be obtained via the start, end, and group methods.
     * @param value CharSequence to check against given regex pattern
     * @return true if, and only if, the entire region sequence matches this matcher's pattern
     */
    fun matches(value: CharSequence?): Boolean {
        return value?.let {
            val matcher = pattern.matcher(it)
            return matcher.matches()
        } ?: false
    }

    /**
     * Returns the input subsequence matched by the previous match.
     * @param value CharSequence to check against given regex pattern
     * @return The (possibly null) subsequence matched by the previous match, in string form
     */
    fun group(value: CharSequence?): String? {
        if (value == null)
            return null

        return try {
            val matcher = pattern.matcher(value)
            if (matcher.find()) matcher.group() else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns the input subsequence captured by the given group during the previous match operation.
     * @param value CharSequence to check against given regex pattern
     * @param groupNo The index of a capturing group in this matcher's pattern
     * @return the (possibly null) subsequence
     */
    fun group(value: CharSequence?, groupNo: Int): String? {
        if (value == null)
            return null

        return try {
            val matcher = pattern.matcher(value)
            if (matcher.find()) matcher.group(groupNo) else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns the input subsequence captured by the named group during the previous match operation.
     * @param value CharSequence to check against given regex pattern
     * @param groupName name of the capture group
     * @return the (possibly null) subsequence
     */
    fun group(value: CharSequence?, groupName: String): String? {
        if (value == null)
            return null

        return try {
            val matcher = pattern.matcher(value)
            if (matcher.find()) matcher.group(groupName) else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Replaces every subsequence of the input sequence that matches the pattern
     * with the given replacement string.
     *
     * @param value CharSequence to check against given regex pattern
     * @param replacement The replacement string
     * @return The string constructed by replacing each matching subsequence by
     * the replacement string, substituting captured subsequences as needed
     */
    fun replaceAll(value: CharSequence?, replacement: String): String? {
        if (value == null)
            return null

        return try {
            val matcher = pattern.matcher(value)
            if (matcher.find()) matcher.replaceAll(replacement) else null
        } catch (e: Exception) {
            null
        }
    }
}