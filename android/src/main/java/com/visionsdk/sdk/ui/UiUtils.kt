package io.packagex.visionsdk.ui

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import io.packagex.visionsdk.exceptions.VisionSDKException
import io.packagex.visionsdk.ui.activities.CreateTemplateActivity
import io.packagex.visionsdk.utils.TAG

private var createTemplateActivityLauncher: ActivityResultLauncher<Intent>? = null

fun ComponentActivity.setTemplateCreatedCallback(onTemplateCreated: (newlyCreatedBarcodeTemplateId: String?) -> Unit) {
    Log.d(TAG, "setTemplateCreatedCallback()")
    createTemplateActivityLauncher = registerForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        callback = { result ->
            if (result.resultCode != Activity.RESULT_OK || result.data == null) {
                onTemplateCreated(null)
                return@registerForActivityResult
            }

            val templateId = result.data?.getStringExtra("RET_CREATED_TEMPLATE")
            onTemplateCreated(templateId)
        }
    )
}

fun ComponentActivity.startCreateTemplateScreen() {
    if (createTemplateActivityLauncher == null) throw VisionSDKException.UnknownException(IllegalStateException("You must call 'setTemplateCreatedCallback()' function in onCreate(), before calling 'startCreateTemplateScreen()'"))

    createTemplateActivityLauncher!!.launch(
        Intent(this, CreateTemplateActivity::class.java)
    )

    createTemplateActivityLauncher = null
}