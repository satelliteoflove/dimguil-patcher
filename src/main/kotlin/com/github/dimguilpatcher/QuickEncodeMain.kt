package com.github.dimguilpatcher

import com.github.dimguilpatcher.encoder.TextEncoderImpl

fun main(args: Array<String>) {
    assert(args.isNotEmpty(), { "No string specified" })

    val encoder = TextEncoderImpl()
    encoder.parseTable("dimguil.tbl")
    val res = encoder.encodePlainString(args[0])
    println(res.joinToString("") { "%02x".format(it) })
}
