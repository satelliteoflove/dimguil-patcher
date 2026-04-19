package com.github.dimguilpatcher.encoder

import com.github.dimguilpatcher.util.Log
import com.github.dimguilpatcher.StringData
import com.github.dimguilpatcher.TranslationUnit
import com.github.dimguilpatcher.util.parseTableInverse
import com.github.dimguilpatcher.patcher.PatcherRule
import com.github.dimguilpatcher.textmanipulation.CompressionStrategy
import com.github.dimguilpatcher.textmanipulation.ControlCodeParser

class TextEncoderImpl(val codeParser: ControlCodeParser, val compression: CompressionStrategy) : TextEncoder {
    private var table: Map<String, UInt> = emptyMap()

    override fun encodeUnit(unit: TranslationUnit): PatcherRule {
        val expandFileBy: Int = unit.extendByBytes ?: 0
        assert(expandFileBy >= 0) { "${unit.file}: expandByBytes must be non-negative." }

        val result = mutableMapOf<ULong, List<Byte>>()
        var extraBytesRequired = 0
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
            if (usedBytes > section.sectionLength) {
                extraBytesRequired += (usedBytes - section.sectionLength).toInt()
                Log.warn("${unit.file}: section ${section.firstHeader} exceeds limit by ${usedBytes - section.sectionLength} bytes. File expansion required.")
            } else {
                Log.info("${unit.file}: section ${section.firstHeader} has ${section.sectionLength.toInt() - usedBytes.toInt()} unused bytes.")
            }
            result.putAll(tempResult)
        }

        if (extraBytesRequired > expandFileBy) {
            Log.err("${unit.file}: exceeded file size + expansion by ${extraBytesRequired - expandFileBy} bytes. Ignoring translation unit.")
            result.clear()
        }
        return PatcherRule(unit.file, result)
    }

    private fun encodeStringData(sd: StringData, compress: Boolean): List<Byte> {
        val s = sd.translation.ifEmpty { sd.source }
        val encodedString = encodePlainString(if (compress) compression.compress(s) else s).toMutableList()
        if (sd.addStringTerminator ?: true) {
            stringTerminatorBytes.forEach { encodedString += it }
        }
        return encodedString
    }

    override fun encodePlainString(s: String): List<Byte> {
        val res = mutableListOf<Byte>()
        var i = 0
        while (i < s.length) {
            val char = s[i]
            when (char) {
                '\n', '\r' -> {
                    val str = escapeSeqs[s[i]]
                    val encoding: UInt =
                        table[str] ?: throw RuntimeException("Unexpected escape sequence: ${s[i].code}")
                    res += ((encoding and 0xff00u) shr 8).toByte()
                    res += (encoding and 0xffu).toByte()
                    i++
                }

                '{' -> {
                    res += codeParser.parse(s, i)
                    while (s[i] != '}') {
                        i++
                    }
                    i++
                }

                '}' -> {
                    throw RuntimeException("Missing brace from hex sequence:\n${s}")
                }

                else -> {
                    val encoding: UInt = table["${s[i]}"] ?: throw RuntimeException("$s - Unknown character: ${s[i]}")
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

    override fun parseTable(tableResource: String) {
        table = parseTableInverse(tableResource, delimiter = '=', Charsets.UTF_8)
    }

    companion object {
        private val stringTerminatorBytes = arrayOf(0xff.toByte(), 0x40.toByte())
        private val escapeSeqs = mapOf(
            '\n' to "\\n",
            '\r' to "\\r"
        )
    }
}
