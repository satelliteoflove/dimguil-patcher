package com.github.dimguilpatcher

import java.nio.charset.Charset

fun main() {
    val sjisTable = parseTable("sjis.tbl", '=', Charset.forName("Shift_JIS"))
    val dimguilTable = parseTableReverse("dimguil.tbl", '=', Charsets.UTF_8)

    var addCount = 0
    var skipCount = 0
    for (i in 0x889fu..0x9ef1u) {
        val sjisChar = sjisTable[i]
        if (sjisChar != null) {
            if (dimguilTable[sjisChar] != null) {
                addCount++
                if (skipCount > 0) {
                    println("-$skipCount")
                    skipCount = 0
                }
            } else {
                skipCount++
                if (addCount > 0) {
                    println("+$addCount")
                    addCount = 0
                }
            }
        }
    }
}