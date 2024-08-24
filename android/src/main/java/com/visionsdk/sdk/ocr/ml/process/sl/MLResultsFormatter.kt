package io.packagex.visionsdk.ocr.ml.process.sl

import com.asadullah.handyutils.capitalizeWords
import com.asadullah.handyutils.removeSpaces
import com.asadullah.handyutils.toLettersOrDigits
import io.packagex.visionsdk.ocr.ml.dto.MLResults
import io.packagex.visionsdk.utils.removeSpecialCharacters

internal class MLResultsFormatter {
    fun format(mlResults: MLResults): MLResults {
        return mlResults.copy(
            packageInfo = mlResults.packageInfo.copy(
                name = mlResults.packageInfo.name.capitalizeWords(),
                trackingNo = mlResults.packageInfo.trackingNo.removeSpaces()!!.uppercase(),
            ),
            sender = mlResults.sender.copy(
                building = mlResults.sender.building.trim().removeSpecialCharacters().capitalizeWords(),
                city = mlResults.sender.city.trim().removeSpecialCharacters().capitalizeWords(),
                country = mlResults.sender.country.capitalizeWords(),
                countryCode = mlResults.sender.countryCode.uppercase(),
                floor = mlResults.sender.floor.trim().removeSpecialCharacters().capitalizeWords(), //TODO: Replace with ordinal number capitalization
                officeNo = mlResults.sender.officeNo.trim().removeSpecialCharacters().capitalizeWords(),
                state = with(mlResults.sender.state) {
                    if (length > 3) capitalizeWords() else uppercase()
                }, //TODO: Fix case
                stateCode = mlResults.sender.stateCode.uppercase(),
                street = mlResults.sender.street.trim().capitalizeWords(), //TODO: Replace with ordinal number capitalization
                zipcode = mlResults.sender.zipcode.trim().removeSpaces()!!.removeSpecialCharacters().uppercase(),
                personBusinessName = mlResults.sender.personBusinessName.capitalizeWords(),
                personName = mlResults.sender.personName.capitalizeWords(),
                poBox = mlResults.sender.poBox.uppercase(),
            ),
            receiver = mlResults.receiver.copy(
                building = mlResults.receiver.building.trim().removeSpecialCharacters().capitalizeWords(),
                city = mlResults.receiver.city.trim().removeSpecialCharacters().capitalizeWords(),
                country = mlResults.receiver.country.capitalizeWords(),
                countryCode = mlResults.receiver.countryCode.uppercase(),
                floor = mlResults.receiver.floor.removeSpecialCharacters().capitalizeWords(), //TODO: Replace with ordinal number capitalization
                officeNo = mlResults.receiver.officeNo.trim().removeSpecialCharacters().capitalizeWords(),
                state = with(mlResults.receiver.state) {
                    if (length > 3) capitalizeWords() else uppercase()
                }, //TODO: Fix case
                stateCode = mlResults.receiver.stateCode.uppercase(),
                street = mlResults.receiver.street.trim().capitalizeWords(), //TODO: Replace with ordinal number capitalization
                zipcode = mlResults.receiver.zipcode.trim().removeSpaces()!!.removeSpecialCharacters().uppercase(),
                personBusinessName = mlResults.receiver.personBusinessName.capitalizeWords(),
                personName = mlResults.receiver.personName.capitalizeWords(),
                poBox = mlResults.receiver.poBox.uppercase(),
            ),
            logisticAttributes = mlResults.logisticAttributes.copy(
                purchaseOrder = mlResults.logisticAttributes.purchaseOrder.uppercase(),
                referenceNumber = mlResults.logisticAttributes.referenceNumber.uppercase(),
                rmaNumber = mlResults.logisticAttributes.rmaNumber.trim().removeSpecialCharacters().uppercase(),
                invoiceNumber = mlResults.logisticAttributes.invoiceNumber.trim().removeSpecialCharacters().uppercase(),
            )
        )
    }
}