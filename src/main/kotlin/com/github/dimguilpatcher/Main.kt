package com.github.dimguilpatcher

import com.github.dimguilpatcher.decoder.TextDecoderImpl
import com.github.dimguilpatcher.encoder.TextEncoderImpl
import com.github.dimguilpatcher.patcher.BinaryPatcherImpl
import com.github.dimguilpatcher.patcher.PatcherRule
import com.github.dimguilpatcher.textmanipulation.ControlCodeParserImpl
import com.github.dimguilpatcher.textmanipulation.DTECompression
import com.github.dimguilpatcher.textmanipulation.NoCompression
import com.github.dimguilpatcher.util.BINARY_PATCHES_PATH
import com.github.dimguilpatcher.util.Log
import com.github.dimguilpatcher.util.TEXT_DUMPS_PATH
import com.github.dimguilpatcher.util.TRANSLATIONS_PATH
import com.github.dimguilpatcher.util.getResource
import com.github.dimguilpatcher.util.json
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.io.path.pathString
import kotlin.io.path.toPath

@OptIn(ExperimentalSerializationApi::class)
fun main(args: Array<String>) {
    when (args[0]) {
        "dump" -> handleDumpText()
        "patch-all" -> handlePatchBinaries()
        "string-encode" -> handleQuickStringEncode(args)

        else -> printUsage()
    }
}

private fun printUsage() {
    println("Usage: [dump|patch-all|string-encode] <string-to-encode>")
}

private fun handleDumpText() {
    val configPath = getResource("config.json")
    val config = json.decodeFromString<Config>(String(Files.readAllBytes(configPath.toURI().toPath())))

    val decoder = TextDecoderImpl(config)
    decoder.parseTable("dimguil.tbl")

    val sectionsResource = getResource("sections.json")
    val jsonString = String(Files.readAllBytes(sectionsResource.toURI().toPath()))
    val metadata: List<SectionConfigData> = json.decodeFromString(jsonString)
    val units = decoder.decode(metadata)

    if (Paths.get(TEXT_DUMPS_PATH).notExists()) {
        Files.createDirectories(Paths.get(TEXT_DUMPS_PATH))
    }

    for (unit in units) {
        val outPath = Paths.get(TEXT_DUMPS_PATH + File.separator + "${unit.file.split(File.separator).last()}.json")
        outPath.toFile().writeText(json.encodeToString(unit).replace("\\\\", "\\"))
    }
}

private fun handlePatchBinaries() {
    val configPath = getResource("config.json")
    val config = json.decodeFromString<Config>(String(Files.readAllBytes(configPath.toURI().toPath())))

    val encoder = TextEncoderImpl(ControlCodeParserImpl("codes.tbl"), DTECompression("compression.tbl"))
    encoder.parseTable("dimguil.tbl")

    val translationRules: List<PatcherRule> = File(TRANSLATIONS_PATH)
        .listFiles { it.extension == "json" }
        ?.map { json.decodeFromString<TranslationUnit>(String(it.readBytes())) }
        ?.map { encoder.encodeUnit(it) }
        ?: emptyList()
    if (translationRules.isEmpty())
        Log.err("No translation units were found in $TRANSLATIONS_PATH.")

    val asmRules: List<PatcherRule> = File(BINARY_PATCHES_PATH)
        .listFiles { it.extension == "json" }
        ?.map { json.decodeFromString<PatcherRule>(String(it.readBytes())) }
        ?: emptyList()
    if (asmRules.isEmpty())
        Log.warn("No binary patches were found in $BINARY_PATCHES_PATH.")

    val combinedRules = (translationRules + asmRules).groupBy { it.file }
        .mapValues { it.value.fold(emptyMap<ULong, List<Byte>>()) { acc, rule -> acc + rule.edits } }
        .mapValues { PatcherRule(it.key, it.value) }

    val patcher = BinaryPatcherImpl(config)
    val outPath = Path(config.targetBinariesPath)
    assert(outPath.exists())
    for (rule in combinedRules) {
        patcher.loadNewSource(rule.key)
        patcher.applyEdits(rule.value.edits)
        val outFile = File("${outPath.pathString}${File.separator}${rule.key}")
        Files.createDirectories(outFile.parentFile.toPath())
        outFile.writeBytes(patcher.result())
    }
}

private fun handleQuickStringEncode(args: Array<String>) {
    if (args.size != 2)
        throw IllegalArgumentException("Invalid arguments")

    val compress = false
    val encoder = TextEncoderImpl(
        ControlCodeParserImpl("codes.tbl"),
        if (compress) DTECompression("compression.tbl") else NoCompression()
    )
    encoder.parseTable("dimguil.tbl")
    val res = encoder.encodePlainString(args[1])
    println(res.joinToString("") { "%02x".format(it) })
}

