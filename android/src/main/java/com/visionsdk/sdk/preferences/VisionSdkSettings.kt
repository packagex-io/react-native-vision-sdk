package io.packagex.visionsdk.preferences

import android.content.Context
import android.content.SharedPreferences
import com.asadullah.handyutils.ifNeitherNullNorEmptyNorBlank
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.packagex.visionsdk.ocr.ml.core.ModelClass
import io.packagex.visionsdk.ocr.ml.core.ModelSize
import io.packagex.visionsdk.preferences.dto.BarcodeTemplate
import io.packagex.visionsdk.preferences.dto.ModelUsage
import io.packagex.visionsdk.service.request.TelemetryData
import io.packagex.visionsdk.utils.toSafeString
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneOffset

internal object VisionSDKSettings {

    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences("vsdk_settings", Context.MODE_PRIVATE)
    }

    fun setLicenseCheckDateTime(dateTime: LocalDateTime) {
        prefs!!
            .edit()
            .putLong("LicenseCheckDateTime", dateTime.toEpochSecond(ZoneOffset.UTC))
            .apply()
    }

    fun getLicenseCheckDateTime(): LocalDateTime {
        return LocalDateTime.ofEpochSecond(
            prefs!!.getLong("LicenseCheckDateTime", -1L),
            0,
            ZoneOffset.UTC
        )
    }

    fun setModelIdAndModelVersionId(modelClass: ModelClass, modelSize: ModelSize, modelId: String, modelVersionId: String) {

        val dataSaved = getDownloadedModelsMetadata()

        if (dataSaved.has(modelClass.value)) {

            val secondLevelData = dataSaved.getJSONObject(modelClass.value)

            if (secondLevelData.has(modelSize.value)) {

                val thirdLevelData = secondLevelData.getJSONObject(modelSize.value)
                thirdLevelData.apply {
                    put("model_id", modelId)
                    put("model_version_id", modelVersionId)
                }

            } else {

                secondLevelData.apply {
                    put(modelSize.value, JSONObject().apply {
                        put("model_id", modelId)
                        put("model_version_id", modelVersionId)
                    })
                }

            }
        } else {
            dataSaved.apply {
                put(modelClass.value, JSONObject().apply {
                    put(modelSize.value, JSONObject().apply {
                        put("model_id", modelId)
                        put("model_version_id", modelVersionId)
                    })
                })
            }
        }

        prefs!!
            .edit()
            .putString("DownloadedModelsMetadata", dataSaved.toSafeString())
            .apply()
    }

    fun clear() {
        prefs!!
            .edit()
            .clear()
            .apply()
    }

    private fun getDownloadedModelsMetadata(): JSONObject {
        return prefs!!
            .getString("DownloadedModelsMetadata", null)
            .ifNeitherNullNorEmptyNorBlank {
                JSONObject(it)
            } ?: JSONObject()
    }

    fun getModelId(modelClass: ModelClass, modelSize: ModelSize): String? {
        return getDownloadedModelsMetadata()
            .optJSONObject(modelClass.value)
            ?.optJSONObject(modelSize.value)
            ?.getString("model_id")
    }

    fun getModelVersionId(modelClass: ModelClass, modelSize: ModelSize): String? {
        return getDownloadedModelsMetadata()
            .optJSONObject(modelClass.value)
            ?.optJSONObject(modelSize.value)
            ?.getString("model_version_id")
    }

    fun onDeviceModelUsageIncrement(modelClass: ModelClass, modelSize: ModelSize, duration: Long) {

        val modelUsages = getOnDeviceModelUsages()

        var modelUsage: ModelUsage? = null
        var indexOfModelUsage: Int? = null

        for ((index, item) in modelUsages.withIndex()) {
            if (item.modelClass == modelClass && item.modelSize == modelSize) {
                modelUsage = item
                indexOfModelUsage = index
                break
            }
        }

        modelUsage = modelUsage?.copy(
            usageCount = modelUsage.usageCount + 1,
            usageDuration = modelUsage.usageDuration + duration
        ) ?: ModelUsage(modelClass, modelSize, 1, duration)

        val updatedList = modelUsages.toMutableList()

        if (indexOfModelUsage == null) {
            updatedList.add(modelUsage)
        } else {
            updatedList[indexOfModelUsage] = modelUsage
        }

        saveOnDeviceModelUsages(updatedList)
    }

    private fun saveOnDeviceModelUsages(modelUsages: List<ModelUsage>) {
        prefs!!
            .edit()
            .putString("ModelUsages", Gson().toJson(modelUsages))
            .apply()
    }

    fun getOnDeviceModelUsages(): List<ModelUsage> {
        return (prefs!!.getString("ModelUsages", null) ?: return emptyList()).let {
            Gson().fromJson(it, object : TypeToken<List<ModelUsage>>() {}.type)
        }
    }

    fun clearOnDeviceModelUsages() {
        prefs!!
            .edit()
            .remove("ModelUsages")
            .apply()
    }

    fun saveBarcodeTemplate(barcodeTemplate: BarcodeTemplate) {
        val allBarcodeTemplates = getAllBarcodeTemplates().toMutableList()
        allBarcodeTemplates.add(barcodeTemplate)
        saveAllBarcodeTemplates(allBarcodeTemplates)
    }

    fun deleteBarcodeTemplate(barcodeTemplate: BarcodeTemplate) {
        val allBarcodeTemplates = getAllBarcodeTemplates().toMutableList()
        allBarcodeTemplates.remove(barcodeTemplate)
        saveAllBarcodeTemplates(allBarcodeTemplates)
    }

    private fun saveAllBarcodeTemplates(allBarcodeTemplates: List<BarcodeTemplate>) {
        prefs!!
            .edit()
            .putString(
                "AllBarcodeTemplates",
                Gson().toJson(allBarcodeTemplates)
            )
            .apply()
    }

    fun getAllBarcodeTemplates(): List<BarcodeTemplate> {
        return prefs!!.getString("AllBarcodeTemplates", null).ifNeitherNullNorEmptyNorBlank {
            Gson().fromJson(it, object : TypeToken<List<BarcodeTemplate>>() {}.type)
        } ?: emptyList()
    }

    fun addTelemetryData(data: TelemetryData) {

        val telemetryDataList = buildList {
            addAll(getTelemetryData())
            add(data)
        }

        prefs!!
            .edit()
            .putString("TelemetryDataList", Gson().toJson(telemetryDataList))
            .apply()
    }

    fun addTelemetryData(data: List<TelemetryData>) {
        val telemetryDataList = getTelemetryData() + data

        prefs!!
            .edit()
            .putString("TelemetryDataList", Gson().toJson(telemetryDataList))
            .apply()
    }

    fun getTelemetryData(): List<TelemetryData> {
        return prefs!!.getString("TelemetryDataList", null).ifNeitherNullNorEmptyNorBlank {
            Gson().fromJson(it, object : TypeToken<List<TelemetryData>>() {}.type)
        } ?: emptyList()
    }

    fun removeTelemetryData(data: List<TelemetryData>) {
        val previousTelemetryData = getTelemetryData()
        val resultingList = previousTelemetryData - data.toSet()
        if (resultingList.isEmpty()) {
            clearTelemetryData()
        } else {
            addTelemetryData(resultingList)
        }
    }

    fun clearTelemetryData() {
        prefs!!
            .edit()
            .clear()
            .apply()
    }
}