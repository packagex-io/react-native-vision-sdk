package io.packagex.visionsdk.preferences

import android.content.Context
import android.content.SharedPreferences
import com.asadullah.handyutils.ifNeitherNullNorEmptyNorBlank
import io.packagex.visionsdk.preferences.dto.BarcodeTemplate
import io.packagex.visionsdk.preferences.dto.BarcodeTemplateData
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneOffset

internal object VisionSdkSettings {

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
                JSONArray().also { barcodeTemplateJsonArray ->
                    allBarcodeTemplates.forEach { barcodeTemplate ->
                        barcodeTemplateJsonArray.put(
                            JSONObject().also { barcodeTemplateJsonObject ->
                                barcodeTemplateJsonObject.put("name", barcodeTemplate.name)
                                barcodeTemplateJsonObject.put(
                                    "data",
                                    JSONArray().also { barcodeTemplateDataJsonArray ->
                                        barcodeTemplate.barcodeTemplateData.forEach { barcodeTemplateData ->
                                            barcodeTemplateDataJsonArray.put(
                                                JSONObject().also { barcodeTemplateDataJsonObject ->
                                                    barcodeTemplateDataJsonObject.put("length", barcodeTemplateData.barcodeLength)
                                                    barcodeTemplateDataJsonObject.put("type", barcodeTemplateData.barcodeFormat)
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        )
                    }
                }.toString()
            )
            .apply()
    }

    fun getAllBarcodeTemplates(): List<BarcodeTemplate> {
        return prefs!!.getString("AllBarcodeTemplates", null)?.ifNeitherNullNorEmptyNorBlank {
            val jsonArray = JSONArray(it)
            buildList {
                for (i in 0 until jsonArray.length()) {
                    val templateAsJson = jsonArray.getJSONObject(i)
                    val templateName = templateAsJson.getString("name")
                    val templateDataJsonArray = templateAsJson.getJSONArray("data")
                    val templateData = buildList {
                        for (j in 0 until templateDataJsonArray.length()) {
                            val templateDataAsJson = templateDataJsonArray.getJSONObject(j)
                            this.add(
                                BarcodeTemplateData(
                                    barcodeLength = templateDataAsJson.getInt("length"),
                                    barcodeFormat = templateDataAsJson.getInt("type")
                                )
                            )
                        }
                    }
                    this.add(BarcodeTemplate(templateName, templateData))
                }
            }
        } ?: emptyList()
    }
}