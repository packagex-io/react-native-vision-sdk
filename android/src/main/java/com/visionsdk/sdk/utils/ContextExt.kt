package io.packagex.visionsdk.utils

import android.content.Context
import android.provider.Settings

fun Context.getAndroidDeviceId() = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)