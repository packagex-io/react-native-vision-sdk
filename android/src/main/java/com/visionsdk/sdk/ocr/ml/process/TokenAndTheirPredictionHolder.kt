package io.packagex.visionsdk.ocr.ml.process

data class TokenAndTheirPredictionHolder(
    val tokens: List<String>, val predictions: List<Int>
) {
    fun getWordsWithPredictions(
        separator: String, wordSeparatorLogic: (token: String) -> Boolean
    ): List<Pair<String, Int>> {

        val wordsWithPredictions = mutableListOf<Pair<String, Int>>()

        var firstCursor = 0
        var secondCursor = 1

        while (firstCursor < tokens.size) {

            while (true) {
                if (secondCursor < tokens.size) {
                    val token = tokens[secondCursor]
                    if (wordSeparatorLogic(token)) {
                        secondCursor++
                        continue
                    } else {
                        break
                    }
                } else {
                    break
                }
            }

            val prediction = predictions[firstCursor]

            val wordBuilder = StringBuilder()
            while (firstCursor < secondCursor) {
                wordBuilder.append(tokens[firstCursor].replace(separator, ""))
                firstCursor++
            }

            val word = wordBuilder.toString()

            wordsWithPredictions.add(Pair(word, prediction))

            secondCursor++
        }

        return wordsWithPredictions
    }
}