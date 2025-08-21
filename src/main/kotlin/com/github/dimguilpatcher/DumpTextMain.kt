package com.github.dimguilpatcher

import com.github.dimguilpatcher.decoder.TextDecoderImpl
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.notExists
import kotlin.io.path.toPath

@OptIn(ExperimentalSerializationApi::class)
fun main() {
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
