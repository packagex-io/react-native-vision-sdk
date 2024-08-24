package io.packagex.visionsdk.ocr.ml.process.sl.micro

import androidx.core.util.component1
import androidx.core.util.component2
import io.packagex.visionsdk.ocr.ml.process.sl.micro.tokenization.FullTokenizer
import kotlin.math.min

class FeatureConverter(inputDictionary: Map<String, Long>) {

    companion object {
        const val MAX_TOKENS = 256
    }

    private val tokenizer: FullTokenizer = FullTokenizer(inputDictionary, true)

    fun convert(textArray: List<String>): Feature {

        val (tokens, subWordMap) = tokenizer.tokenize(textArray)

        val truncatedTokens = listOf(
            "[CLS]",
            *tokens.subList(0, min(tokens.size, MAX_TOKENS - 2)).toTypedArray(),
            "[SEP]"
        )

        val inputIds = tokenizer.convertTokensToIds(truncatedTokens)
        val inputMask = LongArray(inputIds.size) { 1 }.toMutableList()

        while (inputIds.size < MAX_TOKENS) {
            inputIds.add(0)
            inputMask.add(0)
        }

        return Feature(
            inputIds.toLongArray(),
            inputMask.toLongArray(),
            truncatedTokens,
            subWordMap
        )
    }
}