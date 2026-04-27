package com.github.dimguilpatcher.encoder

import com.github.dimguilpatcher.StringData
import com.github.dimguilpatcher.TranslationUnit
import com.github.dimguilpatcher.patcher.PatcherRule
import com.github.dimguilpatcher.textmanipulation.CompressionStrategy
import com.github.dimguilpatcher.textmanipulation.ControlCodeParser
import com.github.dimguilpatcher.util.Log
import com.github.dimguilpatcher.util.parseTableInverse
import java.nio.file.Path

class TextEncoderImpl(val codeParser: ControlCodeParser, val compression: CompressionStrategy) : TextEncoder {
    private var table: Map<String, UInt> = emptyMap()

    override fun encodeUnit(unit: TranslationUnit): PatcherRule {
        val expandFileBy: Int = unit.extendByBytes ?: 0
        if (expandFileBy < 0)
            throw RuntimeException("${unit.file}: expandByBytes must be non-negative.")

        val result = mutableMapOf<ULong, List<Byte>>()
        var extraBytesRequired = 0

        Log.info(unit.file)

        for (section in unit.sections) {
            val tempResult = mutableMapOf<ULong, List<Byte>>()
            var usedBytes = 0u
            var currentHeaderAddress = section.firstHeaderAddress
            var newHeaderAddress = section.firstHeader.toUInt()

            for (stringData in section.strings) {
                val encodedString = encodeStringData(
                    stringData,
                    compress = (stringData.compress == null && section.compress == true) || (stringData.compress == true)
                )
                val newStringAddress = newHeaderAddress + section.firstHeaderAddress
                tempResult += newStringAddress to encodedString
                tempResult += currentHeaderAddress to listOf(
                    (newHeaderAddress and 0xffu).toByte(),
                    (newHeaderAddress shr 8 and 0xffu).toByte()
                )
                currentHeaderAddress += 4u
                val actualLength = encodedString.count() - 2
                newHeaderAddress += encodedString.count().toUInt()
                usedBytes += actualLength.toUInt()
            }

            val sectionHex = "0x%01x".format(section.firstHeaderAddress.toLong())

            if (usedBytes > section.sectionLength) {
                extraBytesRequired += (usedBytes - section.sectionLength).toInt()
                Log.warn("\tsection $sectionHex: exceeds size by ${usedBytes - section.sectionLength} bytes. File expansion may be required.")
            } else {
                Log.info("\tsection $sectionHex: ${section.sectionLength.toInt() - usedBytes.toInt()} unused bytes")
            }
            result.putAll(tempResult)
        }

        if (extraBytesRequired > expandFileBy) {
            Log.err("\t-> exceeded size + expansion by ${extraBytesRequired - expandFileBy} bytes. Ignoring translations.")
            result.clear()
        }
        return PatcherRule(unit.file, result)
    }

    private fun encodeStringData(sd: StringData, compress: Boolean): List<Byte> {
        val string = sd.translation.ifEmpty { sd.source }
        val encodedString = encodePlainString(string, compress).toMutableList()
        if (sd.addStringTerminator ?: true) {
            stringTerminatorBytes.forEach { encodedString += it }
        }
        return encodedString
    }

    override fun encodePlainString(s: String, compress: Boolean): List<Byte> {
        val string = if (compress) compression.compress(s) else s
        val res = mutableListOf<Byte>()
        var i = 0
        while (i < string.length) {
            when (val char = string[i]) {
                '\n', '\r' -> {
                    val escapeSeq = escapeSeqs[char]
                    val encoding = table[escapeSeq] ?: throw RuntimeException("Unexpected escape sequence: ${char.code}")
                    res += ((encoding and 0xff00u) shr 8).toByte()
                    res += (encoding and 0xffu).toByte()
                    i++
                }

                '{' -> {
                    res += codeParser.parse(string, i)
                    while (string[i] != '}') {
                        i++
                    }
                    i++
                }

                '}' -> {
                    throw RuntimeException("Missing '{' from hex/code sequence:\n${string}")
                }

                else -> {
                    val encoding = table["$char"] ?: throw RuntimeException("$string - Unknown character: $char")
                    if (encoding > 0xffu) {
                        res += ((encoding and 0xff00u) shr 8).toByte()
                    }
                    res += (encoding and 0xffu).toByte()
                    i++
                }
            }
        }
        return res
    }

    override fun parseTable(tablePath: Path) {
        table = parseTableInverse(tablePath, delimiter = '=', Charsets.UTF_8)
    }

    companion object {
        private val stringTerminatorBytes = arrayOf(0xff.toByte(), 0x40.toByte())
        private val escapeSeqs = mapOf(
            '\n' to "\\n",
            '\r' to "\\r"
        )
    }
}
