package com.github.dimguilpatcher

import com.github.dimguilpatcher.decoder.TextDecoderImpl
import com.github.dimguilpatcher.encoder.TextEncoderImpl
import com.github.dimguilpatcher.patcher.BinaryPatcherImpl
import com.github.dimguilpatcher.patcher.PatcherRule
import com.github.dimguilpatcher.textmanipulation.ControlCodeParserImpl
import com.github.dimguilpatcher.textmanipulation.DigraphCompression
import com.github.dimguilpatcher.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import kotlin.io.path.bufferedReader
import kotlin.io.path.notExists
import kotlin.system.exitProcess

@OptIn(ExperimentalSerializationApi::class)
fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "dump" -> dumpText()
        "encode-all" -> encodeTranslations()
        "encode-string" -> encodeSingleString(args)

        else -> {
            printUsage()
            exitProcess(-1)
        }
    }
}

private fun printUsage() {
    println("Usage: {dump|encode-all|encode-string} <args>")
}

private fun dumpText() {
    if (CLEAN_DUMP_PATH.notExists())
        throw RuntimeException("Incomplete setup: $CLEAN_DUMP_PATH not found.")

    val decoder = TextDecoderImpl(CLEAN_DUMP_PATH)
    decoder.parseTable(DECODING_TABLE_PATH)

    if (SECTIONS_FILE_PATH.notExists())
        throw FileNotFoundException(SECTIONS_FILE_PATH.toString())

    if (TEXT_DUMPS_PATH.notExists())
        Files.createDirectories(TEXT_DUMPS_PATH)

    val metadata: List<SectionConfigData> = json.decodeFromString(SECTIONS_FILE_PATH.bufferedReader().readText())

    for (dump in decoder.decode(metadata)) {
        Log.info("Processing ${dump.file}...")
        TEXT_DUMPS_PATH
            .resolve("${dump.file.split('/').last()}.json")
            .toFile()
            .writeText(json.encodeToString(dump).replace("\\\\", "\\"))
    }

    Log.info("Done. Text dumped to \"./$TEXT_DUMPS_PATH\"")
}

private fun encodeTranslations() {
    val encoder = TextEncoderImpl(ControlCodeParserImpl(CODES_TABLE_PATH), DigraphCompression(DIGRAPH_TABLE_PATH))
    encoder.parseTable(ENCODING_TABLE_PATH)

    val translationRules: List<PatcherRule> = File(TRANSLATIONS_PATH)
        .listFiles { it.extension == "json" }
        ?.map { json.decodeFromString<TranslationUnit>(String(it.readBytes())) }
        ?.map { encoder.encodeUnit(it) }
        ?: emptyList()
    if (translationRules.isEmpty())
        Log.err("No translation units were found in $TRANSLATIONS_PATH.")

    val binaryRules: List<PatcherRule> = File(BINARY_PATCHES_PATH)
        .listFiles { it.extension == "json" }
        ?.map { json.decodeFromString<PatcherRule>(String(it.readBytes())) }
        ?: emptyList()
    if (binaryRules.isEmpty())
        Log.warn("No binary patches were found in $BINARY_PATCHES_PATH.")

    val combinedRules = (translationRules + binaryRules).groupBy { it.file }
        .mapValues { it.value.fold(emptyMap<ULong, List<Byte>>()) { acc, rule -> acc + rule.edits } }
        .mapValues { PatcherRule(it.key, it.value) }

    if (CLEAN_DUMP_PATH.notExists() || DIRTY_DUMP_PATH.notExists())
        throw RuntimeException("Incomplete setup: $CLEAN_DUMP_PATH and/or $DIRTY_DUMP_PATH not found.")

    val patcher = BinaryPatcherImpl(CLEAN_DUMP_PATH)

    for (rule in combinedRules) {
        patcher.loadNewSource(rule.key)
        patcher.applyEdits(rule.value.edits)
        val outFile = DIRTY_DUMP_PATH.resolve(rule.key).toFile()
        Files.createDirectories(outFile.parentFile.toPath())
        outFile.writeBytes(patcher.result())
    }
}

private fun encodeSingleString(args: Array<String>) {
    if (args.size !in 2..3)
        throw IllegalArgumentException("Invalid arguments")

    var compress = false
    var string = ""

    for (arg in args) {
        if (arg == "-compress") {
            compress = true
        } else if (arg[0] != '-') {
            string = arg
        }
    }

    if (string == "")
        throw IllegalArgumentException("Nothing to encode.")

    val encoder = TextEncoderImpl(ControlCodeParserImpl(CODES_TABLE_PATH), DigraphCompression(DIGRAPH_TABLE_PATH))
    encoder.parseTable(ENCODING_TABLE_PATH)
    println(encoder.encodePlainString(string, compress).joinToString("") { "%02x".format(it) })
}
