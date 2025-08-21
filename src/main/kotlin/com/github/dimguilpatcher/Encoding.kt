package com.github.dimguilpatcher

import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path

private fun getTablePairs(file: URL, delimiter: Char, charset: Charset): List<List<String>> {
    return Files.readAllLines(Path.of(file.toURI()), charset)
        .filter { it.isNotEmpty() }
        .map { it.split(delimiter, limit = 2) }
        .filter { it.size == 2 }
}

fun parseTable(tableResource: String, delimiter: Char, charset: Charset): Map<UInt, String> {
    val file: URL = getResource(tableResource)
    val pairs = getTablePairs(file, delimiter, charset)
    val table = mutableMapOf<UInt, String>()
    for (pair in pairs) {
        val codePoint = pair[0].hexToUInt()
        if (codePoint in table) {
            throw RuntimeException("Code point '${"%x".format(codePoint.toInt())}' has been defined multiple times.")
        }
        table += codePoint to pair[1]
    }
    return table
}

fun parseTableReverse(tableResource: String, delimiter: Char, charset: Charset): Map<String, UInt> {
    val file: URL = getResource(tableResource)
    val pairs = getTablePairs(file, delimiter, charset)
    val table = mutableMapOf<String, UInt>()
    for (pair in pairs) {
        val codePoint = pair[0].hexToUInt()
        if (pair[1] in table) {
            Log.warn("Character '${pair[1]}' has been defined multiple times. Keeping first encoding (${"%x".format(table[pair[1]]?.toInt())}).")
        } else {
            table += pair[1] to codePoint
        }
    }
    return table
}
