/* Code inspired from
https://github.com/huggingface/tflite-android-transformers/blob/dcd6da1bfb28e3cd6bc83b58a112cdcd3d6cc2fe/gpt2/src/main/java/co/huggingface/android_transformers/gpt2/ml/GPT2Client.kt
https://github.com/huggingface/tflite-android-transformers/blob/dcd6da1bfb28e3cd6bc83b58a112cdcd3d6cc2fe/gpt2/src/main/java/co/huggingface/android_transformers/gpt2/tokenization/GPT2Tokenizer.kt
https://github.com/huggingface/tflite-android-transformers/blob/dcd6da1bfb28e3cd6bc83b58a112cdcd3d6cc2fe/gpt2/src/main/java/co/huggingface/android_transformers/gpt2/tokenization/GPT2ByteEncoderDecoder.kt
*/

package io.packagex.visionsdk.ocr.ml.process.sl.large.tokenization

import android.util.Log
import com.asadullah.handyutils.findWithObjectOrNull
import com.asadullah.handyutils.isNeitherNullNorEmptyNorBlank
import io.packagex.visionsdk.ocr.ml.process.sl.large.SLLargeModel
import io.packagex.visionsdk.utils.TAG
import java.text.Normalizer

private val REGEX_REMOVE_ACCENT = "\\p{InCombiningDiacriticalMarks}+".toRegex()

private fun CharSequence.removeAccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return REGEX_REMOVE_ACCENT.replace(temp, "")
}

internal data class TokenWithBoundingBox(val token: String, val tokenId: Long, val boundingBox: List<Int>)

internal class Tokenizer(
    private val encoder: Map<String, Long>,
    private val bpeRanks: List<SLLargeModel.MergesData>
) {
    private val decoder by lazy { encoder.entries.associateBy({ it.value }, { it.key }) }

    private val encodeRegex = Regex("""'s|'t|'re|'ve|'m|'ll|'d| ?\p{L}+| ?\p{N}+| ?[^\s\p{L}\p{N}]+|\s+(?!\S)|\s+""")

    fun decode(tokens: List<Long>): String {
        val text = tokens.joinToString("") { decoder.getOrDefault(it, "") }
        val utfCodepoints = text.map { byteDecoder[it.toString()]!! }
        return String(utfCodepoints.toIntArray(), 0, utfCodepoints.size)
    }

    fun encode(text: String): MutableList<Long> {
        val tokens = encodeRegex.findAll(text).map { result ->
            result.value.codePoints()
                .boxed()
                .map { byteEncoder[it]!! }
                .toArray()
                .joinToString("")
        }

        return tokens
            .map { bpe(it) }
            .flatten()
            .map { encoder[it]!! }
            .toMutableList()
    }

    fun encodeWithBoundingBoxes(textArray: List<String>, boundingBoxes: List<List<Int>>): List<TokenWithBoundingBox> {
        val results = mutableListOf<TokenWithBoundingBox>()

        for ((index, text) in textArray.withIndex()) {

            val correctedText: String = if (index == 0) text else " $text"

            val matchResults = encodeRegex.findAll(correctedText)

            val tokens = mutableListOf<String>()
            for (result in matchResults) {

                val intArray = result.value
                    .removeAccent()
                    .codePoints()
                    .toArray()

                buildString {
                    for (item in intArray) {
                        val encodedValue = byteEncoder.getOrDefault(item, "")
                            .also { char ->
                                if (char == "") {
                                    Log.d(TAG, "Unrecognized character in word: ${result.value}")
                                }
                            }

                        if (encodedValue == "") continue

                        append(encodedValue)
                    }

                }.also {
                    if (it.isNeitherNullNorEmptyNorBlank()) {
                        tokens.add(it)
                    }
                }
            }

            val bpeTokens = tokens.flatMap { bpe(it) }

            for (token in bpeTokens) {
                val encodedToken = encoder[token]!!
                results.add(TokenWithBoundingBox(token, encodedToken, boundingBoxes[index]))
            }
        }

        return results
    }

    private fun bpe(token: String): List<String> {
        if (token.length <= 1) return listOf(token)

        var word = token.map { it.toString() }
        var pairs = getPairs(word)

        while (true) {
//            if (!pairs.any { bpeRanks.containsKey(it) }) break
            if (!pairs.any { pair -> bpeRanks.any { rank -> rank.first == pair.first && rank.second == pair.second } }) break

//            val (first, second) = pairs.minBy { bpeRanks.getOrDefault(it, Int.MAX_VALUE) }
            val (first, second) = pairs.minBy { pair -> bpeRanks.findWithObjectOrNull { rank -> if (rank.first == pair.first && rank.second == pair.second) rank.index.toLong() else null } ?: Long.MAX_VALUE }

            var i = 0
            val newWord = mutableListOf<String>()
            while (i < word.size) {
                val j = word.withIndex().indexOfFirst { it.index >= i && it.value == first }
                if (j != -1) {
                    newWord.addAll(word.subList(i, j))
                    i = j
                } else {
                    newWord.addAll(word.subList(i, word.size))
                    break
                }

                if (word[i] == first && i < word.size - 1 && word[i + 1] == second) {
                    newWord.add(first + second)
                    i += 2
                } else {
                    newWord.add(word[i])
                    i += 1
                }
            }

            word = newWord
            if (word.size == 1) {
                break
            } else {
                pairs = getPairs(word)
            }
        }

        return word
    }

    private fun getPairs(word: List<String>): Set<Pair<String, String>> {
        return buildSet {
            for (i in 0 until word.size - 1) {
                add(word[i] to word[i + 1])
            }
        }
    }

    private val byteEncoder: Map<Int, String> by lazy {
        hashMapOf(
            0 to "\u0100",
            1 to "\u0101",
            2 to "\u0102",
            3 to "\u0103",
            4 to "\u0104",
            5 to "\u0105",
            6 to "\u0106",
            7 to "\u0107",
            8 to "\u0108",
            9 to "\u0109",
            10 to "\u010a",
            11 to "\u010b",
            12 to "\u010c",
            13 to "\u010d",
            14 to "\u010e",
            15 to "\u010f",
            16 to "\u0110",
            17 to "\u0111",
            18 to "\u0112",
            19 to "\u0113",
            20 to "\u0114",
            21 to "\u0115",
            22 to "\u0116",
            23 to "\u0117",
            24 to "\u0118",
            25 to "\u0119",
            26 to "\u011a",
            27 to "\u011b",
            28 to "\u011c",
            29 to "\u011d",
            30 to "\u011e",
            31 to "\u011f",
            32 to "\u0120",
            33 to "!",
            34 to "\"",
            35 to "#",
            36 to "$",
            37 to "%",
            38 to "&",
            39 to "'",
            40 to "(",
            41 to ")",
            42 to "*",
            43 to "+",
            44 to ",",
            45 to "-",
            46 to ".",
            47 to "/",
            48 to "0",
            49 to "1",
            50 to "2",
            51 to "3",
            52 to "4",
            53 to "5",
            54 to "6",
            55 to "7",
            56 to "8",
            57 to "9",
            58 to ":",
            59 to ";",
            60 to "<",
            61 to "=",
            62 to ">",
            63 to "?",
            64 to "@",
            65 to "A",
            66 to "B",
            67 to "C",
            68 to "D",
            69 to "E",
            70 to "F",
            71 to "G",
            72 to "H",
            73 to "I",
            74 to "J",
            75 to "K",
            76 to "L",
            77 to "M",
            78 to "N",
            79 to "O",
            80 to "P",
            81 to "Q",
            82 to "R",
            83 to "S",
            84 to "T",
            85 to "U",
            86 to "V",
            87 to "W",
            88 to "X",
            89 to "Y",
            90 to "Z",
            91 to "[",
            92 to "\\",
            93 to "]",
            94 to "^",
            95 to "_",
            96 to "`",
            97 to "a",
            98 to "b",
            99 to "c",
            100 to "d",
            101 to "e",
            102 to "f",
            103 to "g",
            104 to "h",
            105 to "i",
            106 to "j",
            107 to "k",
            108 to "l",
            109 to "m",
            110 to "n",
            111 to "o",
            112 to "p",
            113 to "q",
            114 to "r",
            115 to "s",
            116 to "t",
            117 to "u",
            118 to "v",
            119 to "w",
            120 to "x",
            121 to "y",
            122 to "z",
            123 to "{",
            124 to "|",
            125 to "}",
            126 to "~",
            127 to "\u0121",
            128 to "\u0122",
            129 to "\u0123",
            130 to "\u0124",
            131 to "\u0125",
            132 to "\u0126",
            133 to "\u0127",
            134 to "\u0128",
            135 to "\u0129",
            136 to "\u012a",
            137 to "\u012b",
            138 to "\u012c",
            139 to "\u012d",
            140 to "\u012e",
            141 to "\u012f",
            142 to "\u0130",
            143 to "\u0131",
            144 to "\u0132",
            145 to "\u0133",
            146 to "\u0134",
            147 to "\u0135",
            148 to "\u0136",
            149 to "\u0137",
            150 to "\u0138",
            151 to "\u0139",
            152 to "\u013a",
            153 to "\u013b",
            154 to "\u013c",
            155 to "\u013d",
            156 to "\u013e",
            157 to "\u013f",
            158 to "\u0140",
            159 to "\u0141",
            160 to "\u0142",
            161 to "\u00a1",
            162 to "\u00a2",
            163 to "\u00a3",
            164 to "\u00a4",
            165 to "\u00a5",
            166 to "\u00a6",
            167 to "\u00a7",
            168 to "\u00a8",
            169 to "\u00a9",
            170 to "\u00aa",
            171 to "\u00ab",
            172 to "\u00ac",
            173 to "\u0143",
            174 to "\u00ae",
            175 to "\u00af",
            176 to "\u00b0",
            177 to "\u00b1",
            178 to "\u00b2",
            179 to "\u00b3",
            180 to "\u00b4",
            181 to "\u00b5",
            182 to "\u00b6",
            183 to "\u00b7",
            184 to "\u00b8",
            185 to "\u00b9",
            186 to "\u00ba",
            187 to "\u00bb",
            188 to "\u00bc",
            189 to "\u00bd",
            190 to "\u00be",
            191 to "\u00bf",
            192 to "\u00c0",
            193 to "\u00c1",
            194 to "\u00c2",
            195 to "\u00c3",
            196 to "\u00c4",
            197 to "\u00c5",
            198 to "\u00c6",
            199 to "\u00c7",
            200 to "\u00c8",
            201 to "\u00c9",
            202 to "\u00ca",
            203 to "\u00cb",
            204 to "\u00cc",
            205 to "\u00cd",
            206 to "\u00ce",
            207 to "\u00cf",
            208 to "\u00d0",
            209 to "\u00d1",
            210 to "\u00d2",
            211 to "\u00d3",
            212 to "\u00d4",
            213 to "\u00d5",
            214 to "\u00d6",
            215 to "\u00d7",
            216 to "\u00d8",
            217 to "\u00d9",
            218 to "\u00da",
            219 to "\u00db",
            220 to "\u00dc",
            221 to "\u00dd",
            222 to "\u00de",
            223 to "\u00df",
            224 to "\u00e0",
            225 to "\u00e1",
            226 to "\u00e2",
            227 to "\u00e3",
            228 to "\u00e4",
            229 to "\u00e5",
            230 to "\u00e6",
            231 to "\u00e7",
            232 to "\u00e8",
            233 to "\u00e9",
            234 to "\u00ea",
            235 to "\u00eb",
            236 to "\u00ec",
            237 to "\u00ed",
            238 to "\u00ee",
            239 to "\u00ef",
            240 to "\u00f0",
            241 to "\u00f1",
            242 to "\u00f2",
            243 to "\u00f3",
            244 to "\u00f4",
            245 to "\u00f5",
            246 to "\u00f6",
            247 to "\u00f7",
            248 to "\u00f8",
            249 to "\u00f9",
            250 to "\u00fa",
            251 to "\u00fb",
            252 to "\u00fc",
            253 to "\u00fd",
            254 to "\u00fe",
            255 to "\u00ff",
        )
    }

    private val byteDecoder by lazy {
        byteEncoder.entries.associateBy({ it.value }) { it.key }
    }
}
