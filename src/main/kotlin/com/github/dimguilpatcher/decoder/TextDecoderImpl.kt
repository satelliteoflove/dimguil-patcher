package com.github.dimguilpatcher.decoder

import com.github.dimguilpatcher.Config
import com.github.dimguilpatcher.Log
import com.github.dimguilpatcher.SectionData
import com.github.dimguilpatcher.StringData
import com.github.dimguilpatcher.TranslationUnit
import com.github.dimguilpatcher.SectionConfigData
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class TextDecoderImpl(private val config: Config) : TextDecoder {
    private val delimiter = '='
    private var table: Map<UInt, String> = emptyMap()

    override fun decode(metadata: List<SectionConfigData>): List<TranslationUnit> {
        val translationUnitTemp = mutableMapOf<String, MutableList<SectionData>>()
        for (metaElement in metadata) {
            if (!translationUnitTemp.contains(metaElement.fileName)) {
                translationUnitTemp[metaElement.fileName] = mutableListOf()
            }
            val file = File(config.gameBinariesPath + File.separator + metaElement.fileName)
            if (!file.exists()) {
                throw IllegalArgumentException("Failed to read $file")
            }
            val bytes = Files.readAllBytes(Path.of(file.toURI()))
            for (headerAddress in metaElement.headerAddresses) {
                if (bytes.count().toUInt() < headerAddress) {
                    throw IllegalArgumentException(
                        "Wrong config for file \"${metaElement.fileName}$\": address 0x${
                            "%x".format(
                                headerAddress
                            )
                        } does not exist."
                    )
                }
                val stringHeaderAddresses = getStringHeaderAddresses(headerAddress, bytes)
                val stringCount = stringHeaderAddresses.count()
                val stringDataList: List<StringData> =
                    decodeSection(headerAddress + (stringCount * 4).toUInt(), bytes, stringCount)
                        .mapIndexed { index, decodedString ->
                            StringData(
                                stringHeaderAddresses[index],
                                decodedString.address,
                                decodedString.length,
                                decodedString.text
                            )
                        }
                if (stringCount != stringDataList.count()) {
                    Log.err("[${metaElement.fileName}][$headerAddress]: ${stringDataList.count()} decoded entries instead of $stringCount")
                }
                translationUnitTemp[metaElement.fileName]!!.add(
                    SectionData(
                        ((bytes[headerAddress.toInt()].toInt() and 0xff) or (bytes[(headerAddress.toInt() + 1)].toInt() shl 8)).toUShort(),
                        stringDataList.sumOf { it.length },
                        stringDataList
                    )
                )
            }
        }
        return translationUnitTemp.map { TranslationUnit(it.key, it.value) }
    }

    private fun getStringHeaderAddresses(firstHeaderAddress: UInt, bytes: ByteArray): List<UInt> {
        val outList = mutableListOf<UInt>()
        for (i in firstHeaderAddress.toInt()..bytes.count() step 4) {
            if (!(bytes[i + 2] == 0.toByte() && bytes[i + 3] == 0.toByte())) {
                break
            }
            outList += i.toUInt()
        }
        return outList
    }

    private fun decodeSection(startAddress: UInt, bytes: ByteArray, stringCount: Int): List<DecodedText> {
        val outList = mutableListOf<DecodedText>()
        var sourceBuilder = StringBuilder()
        var sourceLength = 0u
        var address = startAddress
        var skip = false
        var currentCount = stringCount

        val newString = {
            sourceBuilder = StringBuilder()
            address += sourceLength
            sourceLength = 0u
            currentCount--
        }
        val addDecodedChar = { byteFormat: String, encoding: UInt, bytesToAdd: UInt ->
            val raw = byteFormat.format(encoding.toInt())
            sourceBuilder.append(table[encoding] ?: "{$raw}")
            sourceLength += bytesToAdd
        }

        for (i in startAddress.toInt()..<bytes.count()) {
            if (currentCount == 0) {
                break
            }
            if (skip) {
                skip = false
                continue
            }
            when (val uByte = bytes[i].toUInt() and 0xffu) {
                in 0xf7u..0xffu -> {
                    val doubleByte = (bytes[i].toUInt() shl 8 or (bytes[i + 1].toUInt() and 0xffu)) and 0xffffu
                    if (doubleByte == 0xff40u) {
                        outList += DecodedText(
                            sourceBuilder.toString(),
                            sourceLength,
                            address
                        )
                        sourceLength += 2u
                        newString()
                    } else {
                        addDecodedChar("%04x", doubleByte, 2u)
                    }
                    skip = true
                }
                else -> {
                    addDecodedChar("%02x", uByte, 1u)
                }
            }
        }
        return outList
    }

    override fun parseTable(tableResource: String) {
        table = com.github.dimguilpatcher.parseTable(tableResource, delimiter, Charsets.UTF_8)
    }

    data class DecodedText(val text: String, val length: UInt, val address: UInt)
}
