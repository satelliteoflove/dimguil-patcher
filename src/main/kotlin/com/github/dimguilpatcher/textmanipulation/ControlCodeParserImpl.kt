package com.github.dimguilpatcher.textmanipulation

import com.github.dimguilpatcher.util.Log
import com.github.dimguilpatcher.util.getResource
import com.github.dimguilpatcher.util.getTablePairs
import java.net.URL

class ControlCodeParserImpl(codesPath: String) : ControlCodeParser {
    private val codes: Map<String, String> = readTable(codesPath)

    override fun parse(s: String, start: Int): List<Byte> {
        val end = findEndOfCode(s, start)
        val code = s.substring(start + 1, end)
        try {
            if (codes.contains(code)) {
                return codeToHex(codes[code]!!)
            }
            return codeToHex(code)
        } catch (e: NumberFormatException) {
            throw NumberFormatException("$e for code '$code' in string:\n$s")
        }
    }

    private fun findEndOfCode(s: String, start: Int): Int {
        for (i in start..<s.length) {
            if (s[i] == '}') {
                return i
            }
        }
        throw RuntimeException("Missing '}' in string:\n$s")
    }

    private fun codeToHex(code: String): List<Byte> {
        return code
            .windowed(size = 2, step = 2)
            .map { it.hexToByte() }
    }

    private companion object {
        fun readTable(dteTableResource: String): Map<String, String> {
            val file: URL = getResource(dteTableResource)
            val pairs = getTablePairs(file, '=', Charsets.UTF_8)
            val outTable: Map<String, String> = pairs
                .associate { it[0] to it[1] }
            outTable.forEach {
                if (pairs.count { p -> p[0] == it.key } > 1) {
                    Log.err("Control codes table: sequence '${it.key}' is associated to multiple encodings.")
                }
                if (pairs.count { p -> p[1] == it.value } > 1) {
                    Log.err("Control codes table: encoding '${it.value}' is associated to multiple sequences.")
                }
            }
            return outTable

        }
    }
}
