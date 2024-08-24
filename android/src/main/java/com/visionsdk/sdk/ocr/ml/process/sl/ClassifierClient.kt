package io.packagex.visionsdk.ocr.ml.process.sl

import com.asadullah.handyutils.capitalizeWords
import com.asadullah.handyutils.ifNeitherNullNorEmptyNorBlank
import com.asadullah.handyutils.isNeitherNullNorEmptyNorBlank
import com.asadullah.handyutils.toDigitsWithPoint
import com.asadullah.handyutils.toLetters
import io.packagex.visionsdk.ocr.ml.dto.ExchangeInfo
import io.packagex.visionsdk.ocr.ml.dto.LogisticAttributes
import io.packagex.visionsdk.ocr.ml.dto.MLResults
import io.packagex.visionsdk.ocr.ml.dto.PackageInfo
import io.packagex.visionsdk.ocr.ml.process.LocationProcessor
import io.packagex.visionsdk.utils.removeSpecialCharacters

internal abstract class ClassifierClient {

    protected suspend fun convertDataIntoMLResult(wordsWithPredictions: List<Pair<String, Int>>, locationProcessor: LocationProcessor, lineNumbers: List<Int>): MLResults {

        val intermediateMapWithLabels = mutableMapOf<String, MutableMap<Int, String>>()

        for (label in indexLabels) {
            if (label == "O") continue
            intermediateMapWithLabels[label] = mutableMapOf()
        }

        for ((wordWithPrediction, lineNumber) in wordsWithPredictions.zip(lineNumbers)) {
            val (word, prediction) = wordWithPrediction
            val predictedDictForLabel = intermediateMapWithLabels[indexLabels[prediction]] ?: continue
            val predictedValueForLabel = predictedDictForLabel[lineNumber]
            if (predictedValueForLabel.isNullOrEmpty()) {
                predictedDictForLabel[lineNumber] = word
            } else {
                predictedDictForLabel[lineNumber] = "$predictedValueForLabel $word"
            }
        }

        val mapWithLabels = mutableMapOf<String, String>()

        for ((label, predictionDictionary) in intermediateMapWithLabels) {
            if (predictionDictionary.isEmpty()) continue

            val sortedKeys = predictionDictionary.keys.sorted()
            val concatenatedStringElements = mutableListOf<String>()
            for (blockNumber in sortedKeys) {
                val value = predictionDictionary[blockNumber] ?: continue
                when {
                    deduplicationLabels.contains(label) -> {
                        if (!concatenatedStringElements.contains(value)) {
                            concatenatedStringElements.add(value)
                        }
                        mapWithLabels[label] = concatenatedStringElements.joinToString(" ")
                    }
                    multipleInstanceLabels.contains(label) -> {
                        concatenatedStringElements.add(value.removeSpecialCharacters())
                        mapWithLabels[label] = concatenatedStringElements.joinToString(", ")
                    }
                    else -> {
                        concatenatedStringElements.add(value)
                        mapWithLabels[label] = concatenatedStringElements.joinToString(" ")
                    }
                }
            }
        }

        val senderAdditionalInfo = refineSenderLocation(locationProcessor, mapWithLabels)
        val receiverAdditionalInfo = refineReceiverLocation(locationProcessor, mapWithLabels)

        return MLResults(
            packageInfo = PackageInfo(
                name = mapWithLabels[indexLabels[1]] ?: "",
                trackingNo = mapWithLabels[indexLabels[2]] ?: "",
                dimension = mapWithLabels[indexLabels[3]] ?: "",
                weight = getWeight(mapWithLabels[indexLabels[4]]),
                weightUnit = getWeightUnit(mapWithLabels[indexLabels[4]])
            ),
            sender = ExchangeInfo(
                building = mapWithLabels[indexLabels[17]] ?: "",
                city = mapWithLabels[indexLabels[18]] ?: "",
                country = mapWithLabels[indexLabels[19]] ?: "",
                countryCode = senderAdditionalInfo.countryCode,
                floor = mapWithLabels[indexLabels[20]] ?: "",
                officeNo = mapWithLabels[indexLabels[21]] ?: "",
                state = senderAdditionalInfo.stateName,
                stateCode = senderAdditionalInfo.stateCode,
                street = mapWithLabels[indexLabels[23]] ?: "",
                zipcode = mapWithLabels[indexLabels[24]] ?: "",
                personBusinessName = mapWithLabels[indexLabels[25]] ?: "",
                personName = mapWithLabels[indexLabels[26]] ?: "",
                personPhone = mapWithLabels[indexLabels[27]] ?: "",
                poBox = mapWithLabels[indexLabels[31]] ?: "",
            ),
            receiver = ExchangeInfo(
                building = mapWithLabels[indexLabels[6]] ?: "",
                city = mapWithLabels[indexLabels[7]] ?: "",
                country = mapWithLabels[indexLabels[8]] ?: "",
                countryCode = receiverAdditionalInfo.countryCode,
                floor = mapWithLabels[indexLabels[9]] ?: "",
                officeNo = mapWithLabels[indexLabels[10]] ?: "",
                state = receiverAdditionalInfo.stateName,
                stateCode = receiverAdditionalInfo.stateCode,
                street = mapWithLabels[indexLabels[12]] ?: "",
                zipcode = mapWithLabels[indexLabels[13]] ?: "",
                personBusinessName = mapWithLabels[indexLabels[14]] ?: "",
                personName = mapWithLabels[indexLabels[15]] ?: "",
                personPhone = mapWithLabels[indexLabels[16]] ?: "",
                poBox = mapWithLabels[indexLabels[30]] ?: "",
            ),
            logisticAttributes = LogisticAttributes(
                labelShipmentType = mapWithLabels[indexLabels[5]] ?: "",
                purchaseOrder = mapWithLabels[indexLabels[28]] ?: "",
                referenceNumber = mapWithLabels[indexLabels[29]] ?: "",
                rmaNumber = mapWithLabels[indexLabels[32]] ?: "",
                invoiceNumber = mapWithLabels[indexLabels[33]] ?: "",
            )
        )
    }

    protected fun getIndexOfHighestValue(floatArray: FloatArray): Int {
        val maxValue = floatArray.max()
        return floatArray.indexOfFirst { it == maxValue }
    }

    private fun getWeight(rawWeight: String?): Double {
        return rawWeight.toDigitsWithPoint()?.toDoubleOrNull() ?: 0.0
    }

    private fun getWeightUnit(rawWeight: String?): String {
        return rawWeight.toLetters() ?: ""
    }

    private suspend fun refineSenderLocation(
        locationProcessor: LocationProcessor,
        mapWithLabels: MutableMap<String, String>
    ): AdditionalInfo {

        val additionalInfo = AdditionalInfo()

        var isSenderStateName = false
        var isSenderStateCode = false

        val senderCountryName = mapWithLabels[indexLabels[19]] // S_ADDRESS_COUNTRY
        var refinedCountryName = ""
        senderCountryName.ifNeitherNullNorEmptyNorBlank { value ->
            refinedCountryName = locationProcessor.fuzzySearchCountryName(value)
        }

        val senderCityName = mapWithLabels[indexLabels[18]] // S_ADDRESS_CITY
        val countryNameFromCity = if (senderCityName.isNeitherNullNorEmptyNorBlank()) {
            locationProcessor.getCountryNameFromCityName(senderCityName!!.clean())
        } else emptyList()

        val senderState = mapWithLabels[indexLabels[22]] // S_ADDRESS_STATE
        var countryNameFromState = emptyList<String>()
        if (senderState.isNeitherNullNorEmptyNorBlank()) {
            val countryNameFromStateCode = locationProcessor.getCountryNameFromStateCode(senderState!!)
            if (countryNameFromStateCode.isNotEmpty()) {
                countryNameFromState = countryNameFromStateCode
                isSenderStateCode = true
            } else {
                countryNameFromState = locationProcessor.getCountryNameFromStateName(senderState)
                if (countryNameFromState.isNotEmpty()) {
                    isSenderStateName = true
                }
            }
        }

        val countryNameFinal = locationProcessor.resolveCountryNameFromStateAndCity(
            countryNameListFromCity = countryNameFromCity,
            countryNameListFromState = countryNameFromState,
            countryNamePredictionRefined = refinedCountryName
        )

        mapWithLabels[indexLabels[19]] = countryNameFinal.capitalizeWords()
        additionalInfo.countryCode = locationProcessor.getCountryCodeFromCountryName(countryNameFinal)

        val state = mapWithLabels[indexLabels[22]] // S_ADDRESS_STATE
        val country = mapWithLabels[indexLabels[19]] // S_ADDRESS_COUNTRY

        if (isSenderStateCode) {
            additionalInfo.stateCode = state!!
            additionalInfo.stateName = locationProcessor.getStateNameFromStateCode(state, country!!)
        }

        if (isSenderStateName) {
            additionalInfo.stateName = state!!
            additionalInfo.stateCode = locationProcessor.getStateCodeFromStateName(state, country!!)
        }

        state.ifNeitherNullNorEmptyNorBlank {
            if (isSenderStateCode.not() && isSenderStateName.not()) {
                if (it.length > 3) {
                    additionalInfo.stateName = it
                } else {
                    additionalInfo.stateCode = it
                }
            }
        }

        val senderPhone = mapWithLabels[indexLabels[27]] // S_PERSON_PHONE
        senderPhone.ifNeitherNullNorEmptyNorBlank { value ->
            val updatedSenderCountryName = mapWithLabels[indexLabels[19]] // S_ADDRESS_COUNTRY
            var phoneCode = updatedSenderCountryName.ifNeitherNullNorEmptyNorBlank {
                locationProcessor.getPhoneCodeFromCountryName(it)
            } ?: ""
            val cleanSenderPhone = value.lowercase().removeSpecialCharacters(specialCharacterSet = "!()[]{};:'\",<>.?@#$%^&*_~-").filterNot { it.isWhitespace() }
            if (cleanSenderPhone.startsWith(phoneCode) || cleanSenderPhone.startsWith("+")) {
                phoneCode = ""
            }
            if (phoneCode.startsWith("+") || phoneCode.isEmpty()) {
                mapWithLabels[indexLabels[27]] = "$phoneCode${cleanSenderPhone}"
            } else {
                mapWithLabels[indexLabels[27]] = "+$phoneCode${cleanSenderPhone}"
            }
        }
        return additionalInfo
    }

    private suspend fun refineReceiverLocation(
        locationProcessor: LocationProcessor,
        mapWithLabels: MutableMap<String, String>
    ): AdditionalInfo {

        val additionalInfo = AdditionalInfo()

        var isReceiverStateName = false
        var isReceiverStateCode = false

        val receiverCountryName = mapWithLabels[indexLabels[8]] // R_ADDRESS_COUNTRY
        var refinedCountryName = ""
        receiverCountryName.ifNeitherNullNorEmptyNorBlank { value ->
            refinedCountryName = locationProcessor.fuzzySearchCountryName(value)
        }

        val receiverCityName = mapWithLabels[indexLabels[7]] // R_ADDRESS_CITY
        val countryNameFromCity = if (receiverCityName.isNeitherNullNorEmptyNorBlank()) {
            locationProcessor.getCountryNameFromCityName(receiverCityName!!.clean())
        } else emptyList()

        val receiverState = mapWithLabels[indexLabels[11]] // R_ADDRESS_STATE
        var countryNameFromState = emptyList<String>()
        if (receiverState.isNeitherNullNorEmptyNorBlank()) {
            val countryNameFromStateCode =
                locationProcessor.getCountryNameFromStateCode(receiverState!!)
            if (countryNameFromStateCode.isNotEmpty()) {
                countryNameFromState = countryNameFromStateCode
                isReceiverStateCode = true
            } else {
                countryNameFromState = locationProcessor.getCountryNameFromStateName(receiverState)
                if (countryNameFromState.isNotEmpty()) {
                    isReceiverStateName = true
                }
            }
        }

        val countryNameFinal = locationProcessor.resolveCountryNameFromStateAndCity(
            countryNameListFromCity = countryNameFromCity,
            countryNameListFromState = countryNameFromState,
            countryNamePredictionRefined = refinedCountryName
        )

        mapWithLabels[indexLabels[8]] = countryNameFinal.capitalizeWords()
        additionalInfo.countryCode = locationProcessor.getCountryCodeFromCountryName(countryNameFinal)

        val state = mapWithLabels[indexLabels[11]] // R_ADDRESS_STATE
        val country = mapWithLabels[indexLabels[8]] // R_ADDRESS_COUNTRY

        if (isReceiverStateCode) {
            additionalInfo.stateCode = state!!
            additionalInfo.stateName = locationProcessor.getStateNameFromStateCode(state, country!!)
        }

        if (isReceiverStateName) {
            additionalInfo.stateName = state!!
            additionalInfo.stateCode = locationProcessor.getStateCodeFromStateName(state, country!!)
        }

        state.ifNeitherNullNorEmptyNorBlank {
            if (isReceiverStateCode.not() && isReceiverStateName.not()) {
                if (it.length > 3) {
                    additionalInfo.stateName = it
                } else {
                    additionalInfo.stateCode = it
                }
            }
        }

        val receiverPhone = mapWithLabels[indexLabels[16]] // R_PERSON_PHONE
        receiverPhone.ifNeitherNullNorEmptyNorBlank { value ->
            val updatedReceiverCountryName = mapWithLabels[indexLabels[8]] // R_ADDRESS_COUNTRY
            var phoneCode = updatedReceiverCountryName.ifNeitherNullNorEmptyNorBlank {
                locationProcessor.getPhoneCodeFromCountryName(it)
            } ?: ""
            val cleanReceiverPhone = value.lowercase().removeSpecialCharacters(specialCharacterSet = "!()[]{};:'\",<>.?@#$%^&*_~-").filterNot { it.isWhitespace() }
            if (cleanReceiverPhone.startsWith(phoneCode) || cleanReceiverPhone.startsWith("+")) {
                phoneCode = ""
            }
            if (phoneCode.startsWith("+") || phoneCode.isEmpty()) {
                mapWithLabels[indexLabels[27]] = "$phoneCode${cleanReceiverPhone}"
            } else {
                mapWithLabels[indexLabels[27]] = "+$phoneCode${cleanReceiverPhone}"
            }
        }
        return additionalInfo
    }

    private fun String.clean(): String {
        return this.lowercase().trim().filter { it.isLetterOrDigit() }
    }

    protected val indexLabels = listOf(
        "O", // ----------------------> 0
        "O_COURIER_NAME", // ---------> 1
        "O_COURIER_TRACKING_NO", // --> 2
        "O_EXTRA_DIMENSION", // ------> 3
        "O_EXTRA_WEIGHT", // ---------> 4
        "O_LABEL_SHIPMENT_TYPE", // --> 5
        "R_ADDRESS_BUILDING", // -----> 6
        "R_ADDRESS_CITY", // ---------> 7
        "R_ADDRESS_COUNTRY", // ------> 8
        "R_ADDRESS_FLOOR", // --------> 9
        "R_ADDRESS_OFFICE_NO", // ----> 10
        "R_ADDRESS_STATE", // --------> 11
        "R_ADDRESS_STREET", // -------> 12
        "R_ADDRESS_ZIP_CODE", // -----> 13
        "R_PERSON_BUSINESS_NAME", // -> 14
        "R_PERSON_NAME", // ----------> 15
        "R_PERSON_PHONE", // ---------> 16
        "S_ADDRESS_BUILDING", // -----> 17
        "S_ADDRESS_CITY", // ---------> 18
        "S_ADDRESS_COUNTRY", // ------> 19
        "S_ADDRESS_FLOOR", // --------> 20
        "S_ADDRESS_OFFICE_NO", // ----> 21
        "S_ADDRESS_STATE", // --------> 22
        "S_ADDRESS_STREET", // -------> 23
        "S_ADDRESS_ZIP_CODE", // -----> 24
        "S_PERSON_BUSINESS_NAME", // -> 25
        "S_PERSON_NAME", // ----------> 26
        "S_PERSON_PHONE", // ---------> 27
        "O_PURCHASE_ORDER", // -------> 28
        "O_REFERENCE_NUMBER", // -----> 29
        "R_ADDRESS_PO_BOX", // -------> 30
        "S_ADDRESS_PO_BOX", // -------> 31
        "O_RMA_NUMBER", // -----------> 32
        "O_INVOICE_NUMBER", // -------> 33
        "O_ACCOUNT_ID", // -----------> 34
    )

    protected val deduplicationLabels = listOf(
        "R_ADDRESS_CITY",
        "R_ADDRESS_STATE",
        "R_ADDRESS_ZIP_CODE",
        "R_PERSON_BUSINESS_NAME",
        "R_PERSON_NAME",
        "S_ADDRESS_CITY",
        "S_ADDRESS_STATE",
        "S_ADDRESS_ZIP_CODE",
        "S_PERSON_BUSINESS_NAME",
        "S_PERSON_NAME"
    )

    protected val multipleInstanceLabels = listOf(
        "O_PURCHASE_ORDER",
        "O_REFERENCE_NUMBER"
    )

    data class AdditionalInfo(
        var stateName: String = "",
        var stateCode: String = "",
        var countryCode: String = ""
    )
}