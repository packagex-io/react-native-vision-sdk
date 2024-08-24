package io.packagex.visionsdk.utils

import com.asadullah.handyutils.format
import java.util.Date

fun Date.isoFormat() = Date().format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")