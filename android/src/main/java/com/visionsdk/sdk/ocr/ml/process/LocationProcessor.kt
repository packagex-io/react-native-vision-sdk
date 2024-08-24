package io.packagex.visionsdk.ocr.ml.process

import android.content.Context
import com.asadullah.handyutils.*
import io.packagex.visionsdk.utils.lowercaseAndClean
import io.packagex.visionsdk.utils.removeAccent
import io.packagex.visionsdk.utils.removeDuplicateCharacters
import io.packagex.visionsdk.utils.removeSpecialCharacters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.min

class LocationProcessor(private val context: Context) {

    private val distanceThreshold = 2
    private var countryNameToCountryCodeLowercaseNoWhitespaceNoPunctuationNoDuplicateCharacters = mutableListOf<CountryData>()

    suspend fun init() {
        loadCountryNameToCountryCodeLowercaseNoWhitespaceNoPunctuationNoDuplicateCharacters()
    }

    private suspend fun getMapFromJsonFile(jsonFileName: String): Map<String, *> {
        return withContext(Dispatchers.IO) {
            try {
                val file = context.assets.open("location_jsons/$jsonFileName")
                val bufferedReader = BufferedReader(InputStreamReader(file))
                val stringBuilder = StringBuilder()
                bufferedReader.useLines { lines ->
                    lines.forEach {
                        stringBuilder.append(it)
                    }
                }
                val jsonString = stringBuilder.toString()
                JSONObject(jsonString).toMap()
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

    private suspend fun loadCountryNameToCountryCodeLowercaseNoWhitespaceNoPunctuationNoDuplicateCharacters() {
        if (countryNameToCountryCodeLowercaseNoWhitespaceNoPunctuationNoDuplicateCharacters.isEmpty()) {
            val countryNameToCountryCode: Map<String, String> = getMapFromJsonFile(jsonFileName = "countryNameToCountryCode.json") as Map<String, String>

            countryNameToCountryCode.forEach { (countryName, countryCode) ->

                val cleanCountryName = countryName.lowercaseAndClean()
                val countryNameNoDuplicateCharacters = cleanCountryName.removeDuplicateCharacters()
                val countryCodeLowercase = countryCode.lowercase()

                countryNameToCountryCodeLowercaseNoWhitespaceNoPunctuationNoDuplicateCharacters.add(
                    CountryData(
                        countryNameNoDuplicateCharacters,
                        countryCodeLowercase,
                        countryName
                    )
                )
            }
        }
    }

    fun fuzzySearchCountryName(countryNamePrediction: String): String {

        val cleanCountryNamePrediction = countryNamePrediction.lowercaseAndClean()
        val countryNamePredictionNoDuplicateCharacters = cleanCountryNamePrediction.removeDuplicateCharacters()

        for ((countryName, countryCode, originalName) in countryNameToCountryCodeLowercaseNoWhitespaceNoPunctuationNoDuplicateCharacters) {
            if (listOf(countryName, countryCode).contains(countryNamePredictionNoDuplicateCharacters)) {
                return originalName
            }
        }

        for ((countryName, _, originalName) in countryNameToCountryCodeLowercaseNoWhitespaceNoPunctuationNoDuplicateCharacters) {
            val distance = levenshtein(countryName, countryNamePredictionNoDuplicateCharacters)

            if (distance <= distanceThreshold) {
                if (listOf("us", "usa").contains(countryName)) {
                    return "United States"
                }
                if (countryNamePredictionNoDuplicateCharacters == "uk") {
                    return "United Kingdom"
                }
                if (listOf("it", "italen").contains(countryNamePredictionNoDuplicateCharacters)) {
                    return "Italy"
                }
                if (listOf("de", "deutschlan", "alemgn").contains(countryNamePredictionNoDuplicateCharacters)) {
                    return "Germany"
                }
                if (countryNamePredictionNoDuplicateCharacters == "uae") {
                    return "United Arab Emirates"
                }
                if (countryNamePredictionNoDuplicateCharacters == "ch") {
                    return "Switzerland"
                }
                return originalName
            }
        }

        return countryNamePrediction.removeSpecialCharacters()
    }

    suspend fun getCountryCodeFromCountryName(countryName: String): String {

        val map = getMapFromJsonFile("countryNameToCountryCode.json")
        return map[countryName.capitalizeWords()] as String? ?: ""
    }

    suspend fun getCountryNameFromStateCode(stateCode: String): List<String> {

        val cleanStateCode = stateCode.lowercaseAndClean()
        val map = getMapFromJsonFile("stateCodeToCountryName.json")
        return map[cleanStateCode] as List<String>? ?: emptyList()
    }

    suspend fun getCountryNameFromStateName(stateName: String): List<String> {

        val cleanStateCode = stateName.lowercaseAndClean()
        val map = getMapFromJsonFile("stateNameToStateCode.json")
        val stateNameCodeMap = map[cleanStateCode] as Map<String, String>?
        if (stateNameCodeMap != null) {
            val countryNames = stateNameCodeMap.keys.toMutableSet()
            countryNames.removeIf { it == "clean" }
            return countryNames.toList()
        }

        return emptyList()
    }

    suspend fun getCountryNameFromCityName(cityName: String): List<String> {

        val cleanCityName = cityName.lowercaseAndClean()
        return try {
            val firstTwoLettersOfCity = cleanCityName.substring(0, 2).uppercase().removeAccent()
            val jsonFileName = "cityNameStartsWith$firstTwoLettersOfCity.json"
            val map = getMapFromJsonFile("cityNameToCountryName/$jsonFileName")
            map[cleanCityName] as List<String>? ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun resolveCountryNameFromStateAndCity(
        countryNameListFromCity: List<String>,
        countryNameListFromState: List<String>,
        countryNamePredictionRefined: String
    ): String {

        val countryNamePredictionNoDuplicateCharacters = countryNamePredictionRefined.removeDuplicateCharacters()

        for ((countryName, countryCode, originalName) in countryNameToCountryCodeLowercaseNoWhitespaceNoPunctuationNoDuplicateCharacters) {
            if (listOf(countryName, countryCode).contains(countryNamePredictionNoDuplicateCharacters)) {
                return originalName
            }
        }

        if (countryNameListFromState.size == 1) {
            return countryNameListFromState.first()
        }

        if (countryNameListFromCity.size == 1) {
            return countryNameListFromCity.first()
        }

        if (countryNameListFromState.size > 1 && countryNameListFromCity.size > 1) {

            val sameCountriesByCityAndState = countryNameListFromState.intersect(countryNameListFromCity.toSet())

            if (sameCountriesByCityAndState.size == 1) {
                return sameCountriesByCityAndState.first()
            }

            if (sameCountriesByCityAndState.contains(countryNamePredictionRefined)) {
                return countryNamePredictionRefined
            }

            if (sameCountriesByCityAndState.contains("United States")) {
                return "United States"
            }

            return sameCountriesByCityAndState.first()
        }

        if (countryNameListFromState.size > 1) {

            if (countryNameListFromState.contains(countryNamePredictionRefined)) {
                return countryNamePredictionRefined
            }

            if (countryNameListFromState.contains("United States")) {
                return "United States"
            }

            return countryNameListFromState.first()
        }

        if (countryNameListFromCity.size > 1) {

            if (countryNameListFromCity.contains(countryNamePredictionRefined)) {
                return countryNamePredictionRefined
            }

            if (countryNameListFromCity.contains("United States")) {
                return "United States"
            }

            return countryNameListFromCity.first()
        }

        return countryNamePredictionRefined
    }

    suspend fun getStateCodeFromStateName(stateName: String, countryName: String): String {

        val cleanStateName = stateName.lowercaseAndClean()

        val map = getMapFromJsonFile("stateNameToStateCode.json")
        val stateCodeMap = (map[cleanStateName] as Map<*, *>?) ?: return ""

        return stateCodeMap[countryName.capitalizeWords()] as String? ?: run {
            val countries = stateCodeMap.keys.toMutableSet()
            countries.removeIf { it == "clean" }
            stateCodeMap[countries.first()] as String? ?: ""
        }
    }

    suspend fun getStateNameFromStateCode(stateCode: String, countryName: String): String {
        val map = getMapFromJsonFile("stateCodeToStateName.json")

        val stateNameMap = map[stateCode.uppercase()] as Map<*, *>? ?: return ""
        return stateNameMap[countryName.capitalizeWords()] as String? ?: run {
            val countries = stateNameMap.keys
            stateNameMap[countries.first() as String] as String? ?: ""
        }
    }

    suspend fun getPhoneCodeFromCountryName(countryName: String): String {
        val map = getMapFromJsonFile("countryNameToPhoneCode.json")
        return map[countryName.capitalizeWords()] as String? ?: ""
    }

    private fun levenshtein(lhs : CharSequence, rhs : CharSequence) : Int {
        if(lhs == rhs) { return 0 }
        if(lhs.isEmpty()) { return rhs.length }
        if(rhs.isEmpty()) { return lhs.length }

        val lhsLength = lhs.length + 1
        val rhsLength = rhs.length + 1

        var cost = Array(lhsLength) { it }
        var newCost = Array(lhsLength) { 0 }

        for (i in 1 until rhsLength) {
            newCost[0] = i

            for (j in 1 until lhsLength) {
                val match = if(lhs[j - 1] == rhs[i - 1]) 0 else 1

                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1

                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }

            val swap = cost
            cost = newCost
            newCost = swap
        }

        return cost[lhsLength - 1]
    }

    data class CountryData(val countryName: String, val countryCode: String, val originalName: String)
}