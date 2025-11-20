import { Platform } from 'react-native'
import { VisionCore } from '../../../src/index'

export const syncWithPx = async (scanResult, modelName, env, apiKey) => {
    try {

        const updatedScanResult = JSON.parse(JSON.stringify(scanResult))

        if (env !== 'prod') {
            VisionCore.setEnvironment(env)
        } else {
            VisionCore.setEnvironment('production')
        }

        if (modelName === 'shipping-label') {

            let formatedImageUrl = updatedScanResult?.image_url ?? "" // formatImageUrl(updatedData.image_url) : ''
            if (formatedImageUrl && Platform.OS === 'android' && !formatedImageUrl.startsWith('file')) {
                formatedImageUrl = `file://${formatedImageUrl}`
            }

            updatedScanResult.image_url = formatedImageUrl

            // console.log("FORMATTED IMAGE URL IS: \n", formatedImageUrl)
            // console.log("UPDATED SCAN RESULT IS: \n", JSON.stringify(updatedScanResult))



            const r = await VisionCore.logShippingLabelDataToPx(
                formatedImageUrl ?? '',
                [],
                {
                    data: updatedScanResult
                },
                null,
                apiKey,
                '',
                null,
                {
                    meta1: 'metaval1',
                    meta2: 'metaval2'
                },
                null,
                null,
                true
            )

        } else if (modelName === 'item-label') {
            // updatedScanResult.inference.item.sku = []
            // updatedScanResult.inference.customer.sku = []
            updatedScanResult.inference.item.quantity = updatedScanResult.inference.item.quantity ?
                updatedScanResult.inference.item.quantity.map((item) => parseFloat(item)) : []
            if (updatedScanResult.inference.item.weight.value) {
                updatedScanResult.inference.item.weight.value = parseFloat(updatedScanResult.inference.item.weight.value)
            }
            if (updatedScanResult.inference.package.weight.value) {
                updatedScanResult.inference.package.weight.value = parseFloat(updatedScanResult.inference.package.weight.value)
            }

            if (updatedScanResult.inference.dates.manufacturing.year) {
                updatedScanResult.inference.dates.manufacturing.year = parseInt(updatedScanResult.inference.dates.manufacturing.year)
            }

            if (updatedScanResult.inference.dates.manufacturing.month) {
                updatedScanResult.inference.dates.manufacturing.month = parseInt(updatedScanResult.inference.dates.manufacturing.month)
            }

            if (updatedScanResult.inference.dates.manufacturing.day) {
                updatedScanResult.inference.dates.manufacturing.day = parseInt(updatedScanResult.inference.dates.manufacturing.day)
            }
            if (updatedScanResult.inference.dates.expiry.year) {
                updatedScanResult.inference.dates.expiry.year = parseInt(updatedScanResult.inference.dates.expiry.year)
            }
            if (updatedScanResult.inference.dates.expiry.month) {
                updatedScanResult.inference.dates.expiry.month = parseInt(updatedScanResult.inference.dates.expiry.month)
            }
            if (updatedScanResult.inference.dates.expiry.day) {
                updatedScanResult.inference.dates.expiry.day = parseInt(updatedScanResult.inference.dates.expiry.day)
            }

            if (updatedScanResult.inference.item.dimensions.length.value) {
                updatedScanResult.inference.item.dimensions.length.value = parseFloat(updatedScanResult.inference.item.dimensions.length.value)
            }
            if (updatedScanResult.inference.item.dimensions.width.value) {
                updatedScanResult.inference.item.dimensions.width.value = parseFloat(updatedScanResult.inference.item.dimensions.width.value)
            }
            if (updatedScanResult.inference.item.dimensions.height.value) {
                updatedScanResult.inference.item.dimensions.height.value = parseFloat(updatedScanResult.inference.item.dimensions.height.value)
            }

            if (updatedScanResult.inference.package.dimensions.length.value) {
                updatedScanResult.inference.package.dimensions.length.value = parseFloat(updatedScanResult.inference.package.dimensions.length.value)
            }
            if (updatedScanResult.inference.package.dimensions.width.value) {
                updatedScanResult.inference.package.dimensions.width.value = parseFloat(updatedScanResult.inference.package.dimensions.width.value)
            }
            if (updatedScanResult.inference.package.dimensions.height.value) {
                updatedScanResult.inference.package.dimensions.height.value = parseFloat(updatedScanResult.inference.package.dimensions.height.value)
            }



            let formatedImageUrl = updatedScanResult?.image_url ?? "" // formatImageUrl(updatedData.image_url) : ''

            if (formatedImageUrl && Platform.OS === 'android' && !formatedImageUrl.startsWith("file")) {
                formatedImageUrl = `file://${formatedImageUrl}`
            }


            updatedScanResult.image_url = formatedImageUrl

            if (updatedScanResult.custom) {
                Object.keys(updatedScanResult.custom).forEach(key => {
                    updatedScanResult.inference.additional_attributes[key] = updatedScanResult.custom[key]
                    updatedScanResult.inference.additionalAttributes[key] = updatedScanResult.custom[key]
                })
            }

            const r = await VisionCore.logItemLabelDataToPx(
                formatedImageUrl ?? '',
                [],
                { data: updatedScanResult },
                null,
                apiKey,
                true,
                { meta1: 'meta1val', meta2: 'meta2val' }
            )
        } else {
            return true
        }

    } catch (err) {
        console.log("AN ERROR OCCURED [syncpx]: ", err.message)
        throw err
    }
}