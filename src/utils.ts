const correctOcrEvent = (ocrEvent: any) => {
    if (ocrEvent.data) {
        ocrEvent.data = ocrEvent.data?.data ?? ocrEvent.data ?? null;
    }


    if (ocrEvent.data.inference) {
        if (Object(ocrEvent.data.inference).hasOwnProperty('additionalAttributes')) {
            ocrEvent.data.inference.additional_attributes = ocrEvent.data.inference.additionalAttributes;
        }
        if (Object(ocrEvent.data.inference).hasOwnProperty("additional_attributes")) {
            ocrEvent.data.inference.additionalAttributes = ocrEvent.data.inference.additional_attributes;
        }
        if (Object(ocrEvent.data.inference).hasOwnProperty("barcodeValues")) {
            ocrEvent.data.inference.barcode_values = ocrEvent.data.inference.barcodeValues;
        }
        if (Object(ocrEvent.data.inference).hasOwnProperty("barcode_values")) {
            ocrEvent.data.inference.barcodeValues = ocrEvent.data.inference.barcode_values;
        }
        if (Object(ocrEvent.data.inference).hasOwnProperty("serialNumbers")) {
            ocrEvent.data.inference.serial_numbers = ocrEvent.data.inference.serialNumbers;
        }
        if (Object(ocrEvent.data.inference).hasOwnProperty("serial_numbers")) {
            ocrEvent.data.inference.serialNumbers = ocrEvent.data.inference.serial_numbers;
        }

    } else {
        if (Object(ocrEvent.data).hasOwnProperty("dlVersion")) {
            ocrEvent.data.mvi = ocrEvent.data.dlVersion
        }
        if (Object(ocrEvent.data).hasOwnProperty("dl_version")) {
            ocrEvent.data.mvi = ocrEvent.data.dl_version
        }
        if (Object(ocrEvent.data).hasOwnProperty("mvi")) {
            ocrEvent.data.dlVersion = ocrEvent.data.mvi;
            ocrEvent.data.dl_version = ocrEvent.data.mvi
        }
    }

    return ocrEvent;
}

export { correctOcrEvent };