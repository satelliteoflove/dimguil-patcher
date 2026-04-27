package com.github.dimguilpatcher.util

import java.io.FileNotFoundException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.notExists

fun getTablePairs(path: Path, delimiter: Char, charset: Charset): List<List<String>> {
    if (path.notExists())
        throw FileNotFoundException(path.toString())

    return Files.readAllLines(path, charset)
        .filter { it.isNotEmpty() }
        .map { it.split(delimiter, limit = 2) }
        .filter { it.size == 2 }
}

fun parseTable(tablePath: Path, delimiter: Char, charset: Charset): Map<UInt, String> {
    val pairs = getTablePairs(tablePath, delimiter, charset)
    val table = mutableMapOf<UInt, String>()
    for (pair in pairs) {
        val codePoint = pair[0].hexToUInt()
        if (codePoint in table) {
            Log.err("Code point '${"%x".format(codePoint.toInt())}' has been defined multiple times.")
        }
        table += codePoint to pair[1]
    }
    return table
}

fun parseTableInverse(tablePath: Path, delimiter: Char, charset: Charset): Map<String, UInt> {
    val pairs = getTablePairs(tablePath, delimiter, charset)
    val table = mutableMapOf<String, UInt>()
    for (pair in pairs) {
        val codePoint = pair[0].hexToUInt()
        if (pair[1] in table) {
            Log.warn(
                "Character '${pair[1]}' has been defined multiple times. Keeping first encoding (${
                    "%x".format(
                        table[pair[1]]?.toInt()
                    )
                })."
            )
        } else {
            table += pair[1] to codePoint
        }
    }
    return table
}
