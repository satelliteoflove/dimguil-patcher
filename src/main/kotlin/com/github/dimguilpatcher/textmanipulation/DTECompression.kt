package com.github.dimguilpatcher.textmanipulation

import com.github.dimguilpatcher.util.Log
import com.github.dimguilpatcher.util.getResource
import com.github.dimguilpatcher.util.getTablePairs
import java.net.URL

class DTECompression(dteTableResource: String) : CompressionStrategy {
    private val dteTable: Map<String, String> = readTable(dteTableResource, seqLength = 2)

    override fun compress(s: String): String {
        val sb = StringBuilder()
        var currentWordIdx = 0
        while (currentWordIdx < s.length) {
            val endOfCurrentWordIdx = findEndOfCurrentWord(s, currentWordIdx)
            sb.append(encodeWord(s, currentWordIdx, endOfCurrentWordIdx))
            currentWordIdx += (endOfCurrentWordIdx - currentWordIdx) + 1
        }
        return sb.toString()
    }

    private fun encodeWord(s: String, start: Int, end: Int): String {
        if (start > end || start >= s.length)
            return s

        val sb = StringBuilder()
        var i = start

        fun copyControlBlock() {
            do {
                sb.append(s[i])
                i++
            } while (i <= end && s[i - 1] != '}')
        }

        while (i <= end) {
            when (s[i]) {
                '{' -> copyControlBlock()
                else -> {
                    if (i + 1 <= end) {
                        val seqKey = s[i].toString() + s[i + 1]
                        val code = dteTable[seqKey]

                        if (code != null) {
                            sb.append("{$code}")
                            i += 2
                            continue
                        }
                    }
                    sb.append(s[i])
                    i++
                }
            }
        }

        return sb.toString()
    }

    private fun findEndOfCurrentWord(s: String, start: Int): Int {
        for (i in start..<s.length) {
            if (s[i] == ' ') {
                return i
            }
        }
        return s.length - 1
    }

    private companion object {
        fun readTable(dteTableResource: String, seqLength: Int): Map<String, String> {
            val file: URL = getResource(dteTableResource)
            val pairs = getTablePairs(file, '=', Charsets.UTF_8)
            val outTable: Map<String, String> = pairs
                .filter { it[0].length == seqLength }
                .associate { it[0] to it[1] }
            outTable.forEach {
                if (pairs.count { p -> p[0] == it.key } > 1) {
                    Log.err("DTE table: sequence '${it.key}' is associated to multiple encodings.")
                }
                if (pairs.count { p -> p[1] == it.value } > 1) {
                    Log.err("DTE table: encoding '${it.value}' is associated to multiple sequences.")
                }
            }
            return outTable
        }
    }
}
