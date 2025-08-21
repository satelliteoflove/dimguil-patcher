package com.github.dimguilpatcher.encoder

import com.github.dimguilpatcher.Config
import com.github.dimguilpatcher.Log
import com.github.dimguilpatcher.StringData
import com.github.dimguilpatcher.TranslationUnit
import com.github.dimguilpatcher.parseTableReverse
import com.github.dimguilpatcher.patcher.PatcherRule

class TextEncoderImpl(private val config: Config) : TextEncoder {
    private var table: Map<String, UInt> = emptyMap()

    override fun encode(unit: TranslationUnit): PatcherRule {
        val result = mutableMapOf<UInt, List<Byte>>()
        for (section in unit.sections) {
            val tempResult = mutableMapOf<UInt, List<Byte>>()
            var usedBytes = 0u
            var newHeaderAddress = section.firstHeader.toUInt()
            val toLog = mutableListOf<String>()

            for (stringData in section.strings) {
                val encodedString = encodeString(stringData)
                tempResult += newHeaderAddress to encodedString
                tempResult += stringData.headerAddress to listOf((newHeaderAddress and 0xffu).toByte(), (newHeaderAddress shr 8 and 0xffu).toByte())
                val actualLength = encodedString.count() - 2
                newHeaderAddress += encodedString.count().toUShort()
                usedBytes += actualLength.toUInt()
                if (actualLength.toUInt() > stringData.length) {
                    toLog += "Address ${stringData.stringAddress} -> length exceeded by ${actualLength - stringData.length.toInt()}"
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

    private fun encodeString(sd: StringData): List<Byte> {
        val s = sd.translation.ifEmpty { sd.source }
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
                        val byte = "${s[i + j]}${s[i + j + 1]}".hexToByte()
                        res += byte
                        j += 2
                    }
                    i += j + 1
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
        if (sd.addStringTerminator ?: true) {
            stringTerminatorBytes.forEach { res += it }
        }
        return res
    }

    override fun parseTable(tableResource: String) {
        table = parseTableReverse(tableResource, delimiter = '=', Charsets.UTF_8)
    }

    companion object {
        private val stringTerminatorBytes = arrayOf(0xff.toByte(), 0x40)
        private val escapeSeqs = mapOf(
            '\n' to "\\n",
            '\r' to "\\r"
        )
    }
}
