package com.github.dimguilpatcher.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL

const val OUT_PATH = "out"
const val ASM_EDITS_PATH = "asm"
const val TRANSLATIONS_PATH = "translations"
val TEXT_DUMPS_PATH = OUT_PATH + File.separator + "dumps"
val PATCHED_BINARIES_PATH = OUT_PATH + File.separator + "patched"

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    encodeDefaults = true
    allowComments = true
}

fun getResource(resName: String): URL {
    return Thread.currentThread().contextClassLoader.getResource(resName)
        ?: throw IllegalArgumentException("Failed to read file \"${resName}\".")
}
