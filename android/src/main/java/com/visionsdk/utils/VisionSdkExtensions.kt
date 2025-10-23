package com.visionsdk.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.facebook.react.bridge.ReadableMap
import io.packagex.visionsdk.ocr.ml.core.enums.ModelSize
import io.packagex.visionsdk.ocr.ml.core.enums.OCRModule
import java.io.FileNotFoundException

/**
 * Shared utility functions and extension methods for VisionSDK
 * This file contains helper methods used across multiple VisionSDK components
 */

// MARK: - React Native Bridge Extensions

/**
 * Converts ReadableMap to Map<String, Any> for Kotlin 1.9+ compatibility
 *
 * This is needed because ReadableMap.toHashMap() returns HashMap<String, Any?>
 * but some VisionSDK APIs require Map<String, Any> (non-nullable values).
 * The @Suppress annotation prevents unnecessary cast warnings.
 *
 * @return Map with non-nullable value type
 */
fun ReadableMap.toNonNullableMap(): Map<String, Any> {
    @Suppress("UNCHECKED_CAST")
    return this.toHashMap() as Map<String, Any>
}

// MARK: - Display Unit Conversions

/**
 * Converts pixels to density-independent pixels (DP)
 *
 * @param density The screen density (get from resources.displayMetrics.density)
 * @return The value converted to DP units
 */
fun Int.toDp(density: Float): Int = (this / density + 0.5f).toInt()

// MARK: - Image Processing Utilities

/**
 * Converts a URI to a Bitmap asynchronously
 *
 * This function handles URI to Bitmap conversion with proper error handling.
 * The conversion happens on the calling thread, so use appropriate threading.
 *
 * @param context Android context for accessing content resolver
 * @param uri The URI pointing to the image
 * @param onComplete Callback with the resulting Bitmap, or null if conversion failed
 */
fun uriToBitmap(context: Context, uri: Uri, onComplete: (Bitmap?) -> Unit) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            onComplete(bitmap)
        } else {
            onComplete(null)
        }
    } catch (e: FileNotFoundException) {
        onComplete(null)
    } catch (e: Exception) {
        onComplete(null)
    }
}

// MARK: - Model Type Conversions

/**
 * Converts a model type string to the corresponding OCRModule
 *
 * @param modelTypeStr String representation of the model type
 * @param modelSize The size of the model (affects which OCRModule variant is returned)
 * @return The corresponding OCRModule instance
 */
fun getModelType(modelTypeStr: String?, modelSize: ModelSize?): OCRModule {
    return when (modelTypeStr?.lowercase()) {
        "shipping_label", "shipping-label" -> OCRModule.ShippingLabel(modelSize)
        "bill_of_lading", "bill-of-lading" -> OCRModule.BillOfLading(modelSize)
        "item_label", "item-label" -> OCRModule.ItemLabel(modelSize)
        "document_classification", "document-classification" -> OCRModule.DocumentClassification(modelSize)
        else -> OCRModule.ShippingLabel(modelSize) // Default to Shipping Label
    }
}
