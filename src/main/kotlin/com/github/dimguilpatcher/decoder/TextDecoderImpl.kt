package com.github.dimguilpatcher.decoder

import com.github.dimguilpatcher.Config
import com.github.dimguilpatcher.SectionData
import com.github.dimguilpatcher.StringData
import com.github.dimguilpatcher.TranslationUnit
import com.github.dimguilpatcher.SectionConfigData
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class TextDecoderImpl(private val config: Config) : TextDecoder {
    private var table: Map<UInt, String> = emptyMap()

    override fun decode(metadata: List<SectionConfigData>): List<TranslationUnit> {
        val translationUnitTemp = mutableMapOf<String, MutableList<SectionData>>()
        for (metaElement in metadata) {
            if (!translationUnitTemp.contains(metaElement.file)) {
                translationUnitTemp[metaElement.file] = mutableListOf()
            }
            val file = File(config.gameBinariesPath + File.separator + metaElement.file)
            if (!file.exists()) {
                throw IllegalArgumentException("Failed to read $file")
            }
            val bytes = Files.readAllBytes(Path.of(file.toURI()))
            for (firstHeaderElement in metaElement.headers) {
                val firstHeaderAddress = firstHeaderElement.address
                if (bytes.count().toUInt() < firstHeaderAddress) {
                    throw IllegalArgumentException(
                        "Wrong config for file \"${metaElement.file}$\": address 0x${
                            "%x".format(
                                firstHeaderAddress
                            )
                        } does not exist."
                    )
                }
                val textHeaders = getHeaders(firstHeaderAddress, bytes)
                val stringHeaderAddresses = getHeaderAddresses(firstHeaderAddress, bytes)
                val stringDataList: List<StringData> =
                    decodeSection(bytes, textHeaders.map { it + (firstHeaderElement.offset) })
                        .mapIndexed { index, decodedString ->
                            StringData(
                                stringHeaderAddresses[index],
                                decodedString.address,
                                decodedString.length,
                                decodedString.text,
                                addStringTerminator = decodedString.addTerminator
                            )
                        }
                translationUnitTemp[metaElement.file]!!.add(
                    SectionData(
                        ((bytes[firstHeaderAddress.toInt()].toInt() and 0xff) or (bytes[(firstHeaderAddress.toInt() + 1)].toInt() shl 8)).toUShort(),
                        stringDataList.sumOf { it.length },
                        stringDataList
                    )
                )
            }
        }
        return translationUnitTemp.map { TranslationUnit(it.key, it.value) }
    }

    private fun getHeaderAddresses(firstHeaderAddress: UInt, bytes: ByteArray): List<UInt> {
        val outList = mutableListOf<UInt>()
        for (i in firstHeaderAddress.toInt()..bytes.count() step 4) {
            if (!(bytes[i + 2] == 0.toByte() && bytes[i + 3] == 0.toByte())) {
                break
            }
            outList += i.toUInt()
        }
        return outList
    }

    private fun getHeaders(firstHeaderAddress: UInt, bytes: ByteArray): List<UInt> {
        val outList = mutableListOf<UInt>()
        for (i in firstHeaderAddress.toInt()..bytes.count() step 4) {
            if (!(bytes[i + 2] == 0.toByte() && bytes[i + 3] == 0.toByte())) {
                break
            }
            val hex = (bytes[i].toUInt() and 0xffu) or ((bytes[i + 1].toUInt() shl 8) and 0xff00u)
            outList += hex
        }
        return outList
    }

    private fun decodeSection(bytes: ByteArray, headers: List<UInt>): List<DecodedText> {
        val outList = mutableListOf<DecodedText>()

        for ((hIndex, header) in headers.withIndex()) {
            val originalLength = if (hIndex + 1 < headers.count()) headers[hIndex + 1] - header else NO_LENGTH
            val sourceBuilder = StringBuilder()
            var calculatedLength = 0u
            var addTerminator = true
            var i = header.toInt()
            var end = false

            val addDecodedChar = { byteFormat: String, encoding: UInt, bytesToAdd: UInt ->
                val raw = byteFormat.format(encoding.toInt())
                sourceBuilder.append(table[encoding] ?: "{$raw}")
                calculatedLength += bytesToAdd
                i += bytesToAdd.toInt()
            }

            if (originalLength == 1u) {
                addDecodedChar("%02x", bytes[i].toUInt() and 0xffu, 1u)
                outList += DecodedText(
                    sourceBuilder.toString(),
                    calculatedLength,
                    header,
                    false
                )
                continue
            }

            while (!end && i < bytes.count()) {
                when (val uByte = bytes[i].toUInt() and 0xffu) {
                    in 0xf7u..0xffu -> {
                        val doubleByte = (bytes[i].toUInt() shl 8 or (bytes[i+ 1].toUInt() and 0xffu)) and 0xffffu
                        if (originalLength == NO_LENGTH && doubleByte == 0xff40u) {
                            end = true
                        } else if (calculatedLength == originalLength - 2u) {
                            end = true
                            if (doubleByte != 0xff40u) {
                                addDecodedChar("%04x", doubleByte, 2u)
                                addTerminator = false
                            }
                        } else {
                            addDecodedChar("%04x", doubleByte, 2u)
                        }
                    }
                    else -> {
                        addDecodedChar("%02x", uByte, 1u)
                    }
                }
                if (end) {
                    outList += DecodedText(
                        sourceBuilder.toString(),
                        calculatedLength,
                        header,
                        addTerminator
                    )
                }
            }
        }
        return outList
    }

    override fun parseTable(tableResource: String) {
        table = com.github.dimguilpatcher.util.parseTable(tableResource, DELIMITER, Charsets.UTF_8)
    }

    data class DecodedText(val text: String, val length: UInt, val address: UInt, val addTerminator: Boolean)

    companion object {
        private const val DELIMITER = '='
        private const val NO_LENGTH = UInt.MAX_VALUE
    }
}
