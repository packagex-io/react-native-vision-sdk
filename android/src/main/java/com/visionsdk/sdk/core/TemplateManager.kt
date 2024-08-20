package io.packagex.visionsdk.core

import io.packagex.visionsdk.preferences.VisionSdkSettings
import io.packagex.visionsdk.preferences.dto.BarcodeTemplate

class TemplateManager {
    fun getAllBarcodeTemplates() = VisionSdkSettings.getAllBarcodeTemplates()
    fun saveBarcodeTemplate(barcodeTemplate: BarcodeTemplate) {
        VisionSdkSettings.saveBarcodeTemplate(barcodeTemplate)
    }
    fun deleteBarcodeTemplate(barcodeTemplate: BarcodeTemplate) {
        VisionSdkSettings.deleteBarcodeTemplate(barcodeTemplate)
    }
}