package io.packagex.visionsdk.ocr.ml.process.sl.micro

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import io.packagex.visionsdk.ocr.ml.dto.ExchangeInfo
import io.packagex.visionsdk.ocr.ml.dto.LogisticAttributes
import io.packagex.visionsdk.ocr.ml.dto.MLResults
import io.packagex.visionsdk.ocr.ml.dto.PackageInfo
import io.packagex.visionsdk.ocr.ml.process.sl.micro.FeatureConverter.Companion.MAX_TOKENS
import com.asadullah.handyutils.*
import io.packagex.visionsdk.ocr.ml.process.LocationProcessor
import io.packagex.visionsdk.ocr.ml.process.TokenAndTheirPredictionHolder
import java.nio.LongBuffer

internal class ClassifierClient {

    suspend fun predict(
        vocabulary: Map<String, Long>,
        ortEnvironment: OrtEnvironment,
        ortSession: OrtSession,
        locationProcessor: LocationProcessor,
        ocrExtractedText: String
    ): MLResults {

        val features = FeatureConverter(vocabulary).convert(ocrExtractedText)

        val inputIdsBuffer = LongBuffer.wrap(features.inputIds)
        val attentionMaskBuffer = LongBuffer.wrap(features.inputMask)

        val inputIdsTensor = OnnxTensor.createTensor(
            ortEnvironment,
            inputIdsBuffer,
            longArrayOf(1, MAX_TOKENS.toLong())
        )
        val attentionMaskTensor = OnnxTensor.createTensor(
            ortEnvironment,
            attentionMaskBuffer,
            longArrayOf(1, MAX_TOKENS.toLong())
        )

        val outputArray: Array<Array<FloatArray>>

        inputIdsTensor.use {
            attentionMaskTensor.use {
                val inputMap = mapOf(
                    "input_ids" to inputIdsTensor,
                    "attention_mask" to attentionMaskTensor
                )
                val output = ortSession.run(inputMap)
                output.use {
                    outputArray = (output?.get(0)?.value as Array<Array<FloatArray>>)
                }
            }
        }

        val predictions = mutableListOf<Int>()
        val predictionsInString = mutableListOf<String>()
        outputArray[0].forEach {
            val index = getIndexOfHighestValue(it)
            predictions.add(index)
            predictionsInString.add(indexLabels[index])
        }

        val allTokensWithoutCLSAndSEP = features.origTokens.subList(1, features.origTokens.size - 2)
        val allPredictionsWithoutThePredictionsOfCLSAndSEP = predictions.subList(1, features.origTokens.size - 2)

        val tokenAndTheirPredictionHolder = TokenAndTheirPredictionHolder(
            allTokensWithoutCLSAndSEP,
            allPredictionsWithoutThePredictionsOfCLSAndSEP
        )

        val wordsWithPredictions = tokenAndTheirPredictionHolder.getWordsWithPredictions("##") { token ->
            token.startsWith("##")
        }

        val mapWithLabels = mutableMapOf<String, String>()
        val mapWithIndices = mutableMapOf<Int, String>()

        wordsWithPredictions.forEach { (word, prediction) ->
            if (prediction == 0) return@forEach

            if (mapWithLabels.containsKey(indexLabels[prediction])) {
                mapWithLabels[indexLabels[prediction]] += " $word"
                mapWithIndices[prediction] += " $word"
            } else {
                mapWithLabels[indexLabels[prediction]] = word
                mapWithIndices[prediction] = word
            }
        }

        val senderAdditionalInfo = refineSenderLocation(locationProcessor, mapWithLabels)
        val receiverAdditionalInfo = refineReceiverLocation(locationProcessor, mapWithLabels)

        return MLResults(
            packageInfo = PackageInfo(
                name = mapWithIndices[1] ?: "",
                trackingNo = mapWithIndices[2] ?: "",
                dimension = mapWithIndices[3] ?: "",
                weight = getWeight(mapWithIndices[4]),
                weightUnit = getWeightUnit(mapWithIndices[4])
            ),
            sender = ExchangeInfo(
                building = mapWithIndices[17] ?: "",
                city = mapWithIndices[18] ?: "",
                country = mapWithIndices[19] ?: "",
                countryCode = senderAdditionalInfo.countryCode,
                floor = mapWithIndices[20] ?: "",
                officeNo = mapWithIndices[21] ?: "",
                state = senderAdditionalInfo.stateName.toNullIfEmptyOrBlank() ?: mapWithIndices[22] ?: "",
                stateCode = senderAdditionalInfo.stateCode,
                street = mapWithIndices[23] ?: "",
                zipcode = mapWithIndices[24] ?: "",
                personBusinessName = mapWithIndices[25] ?: "",
                personName = mapWithIndices[26] ?: "",
                personPhone = mapWithIndices[27] ?: "",
                poBox = mapWithIndices[31] ?: "",
            ),
            receiver = ExchangeInfo(
                building = mapWithIndices[6] ?: "",
                city = mapWithIndices[7] ?: "",
                country = mapWithIndices[8] ?: "",
                countryCode = receiverAdditionalInfo.countryCode,
                floor = mapWithIndices[9] ?: "",
                officeNo = mapWithIndices[10] ?: "",
                state = receiverAdditionalInfo.stateName.toNullIfEmptyOrBlank() ?: mapWithIndices[11] ?: "",
                stateCode = senderAdditionalInfo.stateCode,
                street = mapWithIndices[12] ?: "",
                zipcode = mapWithIndices[13] ?: "",
                personBusinessName = mapWithIndices[14] ?: "",
                personName = mapWithIndices[15] ?: "",
                personPhone = mapWithIndices[16] ?: "",
                poBox = mapWithIndices[30] ?: "",
            ),
            logisticAttributes = LogisticAttributes(
                labelShipmentType = mapWithIndices[5] ?: "",
                purchaseOrder = mapWithIndices[28] ?: "",
                referenceNumber = mapWithIndices[29] ?: "",
                rmaNumber = mapWithIndices[32] ?: "",
                invoiceNumber = mapWithIndices[33] ?: "",
            )
        )
    }

    private fun getIndexOfHighestValue(floatArray: FloatArray): Int {
        val maxValue = floatArray.max()
        return floatArray.indexOfFirst { it == maxValue }
    }

    private fun getWeight(rawWeight: String?): Double {
        return rawWeight.toDigitsWithPoint()?.toDoubleOrNull() ?: 0.0
    }

    private fun getWeightUnit(rawWeight: String?): String {
        return rawWeight.toLetters() ?: ""
    }

    private suspend fun refineSenderLocation(locationProcessor: LocationProcessor, mapWithLabels: MutableMap<String, String>): AdditionalInfo {

        val additionalInfo = AdditionalInfo()

        var isSenderStateName = false
        var isSenderStateCode = false

        val senderCountryName = mapWithLabels[indexLabels[19]] // S_ADDRESS_COUNTRY
        senderCountryName.ifNeitherNullNorEmptyNorBlank { value ->
            val refinedCountryName = locationProcessor.fuzzySearchCountryName(value.clean())

            val senderCityName = mapWithLabels[indexLabels[18]] // S_ADDRESS_CITY
            val countryNameFromCity = if (senderCityName.isNeitherNullNorEmptyNorBlank()) {
                locationProcessor.getCountryNameFromCityName(senderCityName!!.clean())
            } else emptyList()

            val senderState = mapWithLabels[indexLabels[23]] // S_ADDRESS_STATE
            val countryNameFromState = if (senderState.isNeitherNullNorEmptyNorBlank()) {

                val countryNameFromState = locationProcessor.getCountryNameFromStateCode(senderState!!)
                if (countryNameFromState.isNotEmpty()) {
                    isSenderStateCode = true
                    countryNameFromState
                } else {
                    isSenderStateName = true
                    locationProcessor.getCountryNameFromStateName(senderState)
                }
            } else emptyList()

            val countryNameFinal = locationProcessor.resolveCountryNameFromStateAndCity(
                countryNameListFromCity = countryNameFromCity,
                countryNameListFromState = countryNameFromState,
                countryNamePredictionRefined = refinedCountryName
            )

            mapWithLabels[indexLabels[19]] = countryNameFinal.capitalizeWords()
            additionalInfo.countryCode = locationProcessor.getCountryCodeFromCountryName(countryNameFinal)
        }

        val state = mapWithLabels[indexLabels[22]] // S_ADDRESS_STATE
        val country = mapWithLabels[indexLabels[19]] // S_ADDRESS_COUNTRY
        if (state.isNeitherNullNorEmptyNorBlank() && country.isNeitherNullNorEmptyNorBlank()) {

            if (isSenderStateCode) {
                additionalInfo.stateName = locationProcessor.getStateNameFromStateCode(state!!, country!!)
            }

            if (isSenderStateName) {
                additionalInfo.stateCode = locationProcessor.getStateCodeFromStateName(state!!, country!!)
            }
        }

        val senderPhoneCode = mapWithLabels[indexLabels[27]] // S_PERSON_PHONE
        senderPhoneCode.ifNeitherNullNorEmptyNorBlank { value ->
            val updatedSenderCountryName = mapWithLabels[indexLabels[19]] // S_ADDRESS_COUNTRY
            val phoneCode = updatedSenderCountryName.ifNeitherNullNorEmptyNorBlank { locationProcessor.getPhoneCodeFromCountryName(it) } ?: ""
            val cleanSenderPhoneCode = value.lowercase().trim().filter { it.isLetterOrDigit() || it == '+' }
            if (cleanSenderPhoneCode.startsWith(phoneCode).not() && cleanSenderPhoneCode.startsWith("+").not()) {
                val updatedPhoneNo = if (phoneCode.startsWith("+")) {
                    "$phoneCode$cleanSenderPhoneCode"
                } else {
                    "+$phoneCode$cleanSenderPhoneCode"
                }
                mapWithLabels[indexLabels[27]] = updatedPhoneNo // S_PERSON_PHONE
            }
        }

        return additionalInfo
    }

    private suspend fun refineReceiverLocation(locationProcessor: LocationProcessor, mapWithLabels: MutableMap<String, String>): AdditionalInfo {

        val additionalInfo = AdditionalInfo()

        var isReceiverStateName = false
        var isReceiverStateCode = false

        val receiverCountryName = mapWithLabels[indexLabels[8]] // R_ADDRESS_COUNTRY
        receiverCountryName.ifNeitherNullNorEmptyNorBlank { value ->
            val refinedCountryName = locationProcessor.fuzzySearchCountryName(value.clean())

            val receiverCityName = mapWithLabels[indexLabels[7]] // R_ADDRESS_CITY
            val countryNameFromCity = if (receiverCityName.isNeitherNullNorEmptyNorBlank()) {
                locationProcessor.getCountryNameFromCityName(receiverCityName!!.clean())
            } else emptyList()

            val receiverState = mapWithLabels[indexLabels[11]] // R_ADDRESS_STATE
            val countryNameFromState = if (receiverState.isNeitherNullNorEmptyNorBlank()) {

                val countryNameFromState = locationProcessor.getCountryNameFromStateCode(receiverState!!)
                if (countryNameFromState.isNotEmpty()) {
                    isReceiverStateCode = true
                    countryNameFromState
                } else {
                    isReceiverStateName = true
                    locationProcessor.getCountryNameFromStateName(receiverState)
                }
            } else emptyList()

            val countryNameFinal = locationProcessor.resolveCountryNameFromStateAndCity(
                countryNameListFromCity = countryNameFromCity,
                countryNameListFromState = countryNameFromState,
                countryNamePredictionRefined = refinedCountryName
            )

            mapWithLabels[indexLabels[8]] = countryNameFinal.capitalizeWords()
            additionalInfo.countryCode = locationProcessor.getCountryCodeFromCountryName(countryNameFinal)
        }

        val state = mapWithLabels[indexLabels[11]] // R_ADDRESS_STATE
        val country = mapWithLabels[indexLabels[8]] // R_ADDRESS_COUNTRY
        if (state.isNeitherNullNorEmptyNorBlank() && country.isNeitherNullNorEmptyNorBlank()) {

            if (isReceiverStateCode) {
                additionalInfo.stateName = locationProcessor.getStateNameFromStateCode(state!!, country!!)
            }

            if (isReceiverStateName) {
                additionalInfo.stateCode = locationProcessor.getStateCodeFromStateName(state!!, country!!)
            }
        }

        val receiverPhoneCode = mapWithLabels[indexLabels[16]] // R_PERSON_PHONE
        receiverPhoneCode.ifNeitherNullNorEmptyNorBlank { value ->
            val updatedReceiverCountryName = mapWithLabels[indexLabels[8]] // R_ADDRESS_COUNTRY
            val phoneCode = updatedReceiverCountryName.ifNeitherNullNorEmptyNorBlank { locationProcessor.getPhoneCodeFromCountryName(it) } ?: ""
            val cleanReceiverPhoneCode = value.lowercase().trim().filter { it.isLetterOrDigit() || it == '+' }
            if (cleanReceiverPhoneCode.startsWith(phoneCode).not() && cleanReceiverPhoneCode.startsWith("+").not()) {
                val updatedPhoneNo = if (phoneCode.startsWith("+")) {
                    "$phoneCode$cleanReceiverPhoneCode"
                } else {
                    "+$phoneCode$cleanReceiverPhoneCode"
                }
                mapWithLabels[indexLabels[16]] = updatedPhoneNo // R_PERSON_PHONE
            }
        }

        return additionalInfo
    }

    private fun String.clean(): String {
        return this.lowercase().trim().filter { it.isLetterOrDigit() }
    }

    private val indexLabels = listOf(
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

    private data class AdditionalInfo(
        var countryCode: String = "",
        var stateCode: String = "",
        var stateName: String = ""
    )
}