package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal val patternOtherTracking1 by lazy {
    VisionRegex("(?i)^([A-Z]{2}|[A-Z]{5}) *( *[\\d]){9} *([A-Z]{2})\$", RegexType.TrackingNo)
}

internal val patternOtherTracking2 by lazy {
    VisionRegex("(?i)^([A-Z]{1,6}) *( *[\\d]){5,} *\$", RegexType.TrackingNo)
}

internal val patternOtherTracking3 by lazy {
    VisionRegex("(?i)^[\\d\\- ]{8,}\$", RegexType.TrackingNo)
}

internal val patternOtherTracking4 by lazy {
    VisionRegex("(?i)^[A-Z0-9]{6,}\$", RegexType.TrackingNo)
}

internal val patternOtherNorway by lazy {
    VisionRegex("\\(?00\\)?(\\s*3707\\s*\\d{14})(\$|\\b)", RegexType.Default)
}

internal val patternOtherWeWork by lazy {
    VisionRegex("(?i)^[A-Z0-9]{4}\$", RegexType.Default)
}