package com.github.dimguilpatcher.encoder

import com.github.dimguilpatcher.util.Log
import com.github.dimguilpatcher.StringData
import com.github.dimguilpatcher.TranslationUnit
import com.github.dimguilpatcher.util.parseTableReverse
import com.github.dimguilpatcher.patcher.PatcherRule

class TextEncoderImpl() : TextEncoder {
    private var table: Map<String, UInt> = emptyMap()

    override fun encodeUnit(unit: TranslationUnit): PatcherRule {
        val result = mutableMapOf<UInt, List<Byte>>()
        for (section in unit.sections) {
            val tempResult = mutableMapOf<UInt, List<Byte>>()
            var usedBytes = 0u
            var currentHeaderAddress = section.firstHeaderAddress.toUInt()
            var newHeaderAddress = section.firstHeader.toUInt()
            val toLog = mutableListOf<String>()

            for (stringData in section.strings) {
                val encodedString = encodeStringData(stringData)
                val newStringAddress = newHeaderAddress + section.firstHeaderAddress
                tempResult += newStringAddress to encodedString
                tempResult += currentHeaderAddress to listOf((newHeaderAddress and 0xffu).toByte(), (newHeaderAddress shr 8 and 0xffu).toByte())
                currentHeaderAddress += 4u
                val actualLength = encodedString.count() - 2
                newHeaderAddress += encodedString.count().toUShort()
                usedBytes += actualLength.toUInt()
                if (actualLength.toUInt() > stringData.length) {
                    toLog += "String at header $currentHeaderAddress -> length exceeded by ${actualLength.toUInt() - stringData.length}"
                }
            }
            if (usedBytes > section.sectionLength) {
                Log.warn("${unit.file}: section ${section.firstHeader} exceeds limit by ${usedBytes - section.sectionLength} bytes. Ignoring.")
                toLog.forEach { Log.info("- $it") }
            } else {
                Log.info("${unit.file}: section ${section.firstHeader} has ${section.sectionLength - usedBytes} bytes left.")
                result.putAll(tempResult)
            }
        }
        return PatcherRule(unit.file, result)
    }

    private fun encodeStringData(sd: StringData): List<Byte> {
        val encodedString = encodePlainString(sd.translation.ifEmpty { sd.source }).toMutableList()
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
                    val encoding: UInt = table[str] ?: throw RuntimeException("Unexpected escape sequence: ${s[i].code}")
                    res += ((encoding and 0xff00u) shr 8).toByte()
                    res += (encoding and 0xffu).toByte()
                    i++
                }
                '{' -> {
                    var j = 1
                    while (s[i + j] != '}') {
                        assert(j <= HEX_SEQUENCE_MAX_LENGTH) { "Hex sequence greater than 2 bytes:\n${s}" }
                        val byte = "${s[i + j]}${s[i + j + 1]}".hexToByte()
                        res += byte
                        j += 2
                    }
                    i += j + 1
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
        table = parseTableReverse(tableResource, delimiter = '=', Charsets.UTF_8)
    }

    companion object {
        private const val HEX_SEQUENCE_MAX_LENGTH = 4
        private val stringTerminatorBytes = arrayOf(0xff.toByte(), 0x40)
        private val escapeSeqs = mapOf(
            '\n' to "\\n",
            '\r' to "\\r"
        )
    }
}
