package io.packagex.visionsdk.ocr.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.packagex.visionsdk.ocr.ml.core.ModelClass
import io.packagex.visionsdk.ocr.ml.core.OnDeviceOCRManager
import io.packagex.visionsdk.utils.TAG

internal class TimeChangeBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        if (context == null || intent == null || (intent.action != "android.intent.action.TIME_SET" && intent.action != "android.intent.action.TIMEZONE_CHANGED")) {
            return
        }

        Log.d(TAG, "Time change detected, securing files...")
        OnDeviceOCRManager(context, modelClass = ModelClass.ShippingLabel).permanentlyDeleteAllModels()
        Log.d(TAG, "Securing files successful.")
    }
}