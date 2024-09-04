package io.packagex.visionsdk.core

import io.packagex.visionsdk.preferences.VisionSDKSettings
import io.packagex.visionsdk.preferences.dto.BarcodeTemplate

class TemplateManager {
    fun getAllBarcodeTemplates() = VisionSDKSettings.getAllBarcodeTemplates()
    fun saveBarcodeTemplate(barcodeTemplate: BarcodeTemplate) {
        VisionSDKSettings.saveBarcodeTemplate(barcodeTemplate)
    }
    fun deleteBarcodeTemplate(barcodeTemplate: BarcodeTemplate) {
        VisionSDKSettings.deleteBarcodeTemplate(barcodeTemplate)
    }
}