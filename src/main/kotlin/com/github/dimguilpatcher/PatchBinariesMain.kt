package com.github.dimguilpatcher

import com.github.dimguilpatcher.encoder.TextEncoderImpl
import com.github.dimguilpatcher.patcher.BinaryPatcherImpl
import com.github.dimguilpatcher.patcher.PatcherRule
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists
import kotlin.io.path.toPath

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val configPath = getResource("config.json")
    val config = json.decodeFromString<Config>(String(Files.readAllBytes(configPath.toURI().toPath())))

    val asmRules: List<PatcherRule>? = File(ASM_EDITS_PATH)
        .listFiles { it.extension == "json" }
        ?.map { json.decodeFromString<PatcherRule>(String(it.readBytes())) }

    val encoder = TextEncoderImpl(config)
    encoder.parseTable("dimguil.tbl")
    val translationRules: List<PatcherRule> = File(TRANSLATIONS_PATH)
        .listFiles { it.extension == "json" }!!
        .map { json.decodeFromString<TranslationUnit>(String(it.readBytes())) }
        .map { encoder.encode(it) }

    val allRules = translationRules.toMutableList()
    if (asmRules != null) {
        allRules += asmRules
    }

    val combinedRules = allRules.groupBy { it.file }
        .mapValues { it.value.fold(emptyMap<UInt, List<Byte>>()) { acc, rule -> acc + rule.edits } }
        .mapValues { PatcherRule(it.key, it.value) }

    val patcher = BinaryPatcherImpl(config)
    val outPath = Path(PATCHED_BINARIES_PATH)
    if (outPath.notExists()) {
        outPath.createDirectories()
    }
    for (rule in combinedRules) {
        patcher.loadNewSource(rule.key)
        patcher.applyEdits(rule.value.edits)
        val outFile = File("$PATCHED_BINARIES_PATH${File.separator}${rule.key}")
        Files.createDirectories(outFile.parentFile.toPath())
        outFile.writeBytes(patcher.result())
    }
}