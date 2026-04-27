package com.github.dimguilpatcher.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.nio.file.Path

const val OUT_PATH = "out"
const val BINARY_PATCHES_PATH = "binary"
const val TRANSLATIONS_PATH = "translations"
val TEXT_DUMPS_PATH: Path = Path.of(OUT_PATH, "dumps")

const val ROOT_DUMP_PATH = "rips"
val CLEAN_DUMP_PATH: Path = Path.of(ROOT_DUMP_PATH, "clean", "dimguil")
val DIRTY_DUMP_PATH: Path = Path.of(ROOT_DUMP_PATH, "dirty", "dimguil")

val SECTIONS_FILE_PATH: Path = Path.of("sections.json")

const val TABLES_ROOT = "tables"
val DECODING_TABLE_PATH: Path = Path.of(TABLES_ROOT, "dimguil_dec.tbl")
val ENCODING_TABLE_PATH: Path = Path.of(TABLES_ROOT, "dimguil_enc.tbl")
val CODES_TABLE_PATH: Path = Path.of(TABLES_ROOT, "codes.tbl")
val DIGRAPH_TABLE_PATH: Path = Path.of(TABLES_ROOT, "compression.tbl")

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    allowComments = true
    encodeDefaults = true
    //explicitNulls = false
    prettyPrint = true
}
