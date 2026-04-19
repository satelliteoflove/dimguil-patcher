package com.github.dimguilpatcher.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL

const val OUT_PATH = "out"
const val BINARY_PATCHES_PATH = "binary"
const val TRANSLATIONS_PATH = "translations"
val TEXT_DUMPS_PATH = OUT_PATH + File.separator + "dumps"

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    allowComments = true
    encodeDefaults = true
    //explicitNulls = false
    prettyPrint = true
}

fun getResource(resName: String): URL {
    return Thread.currentThread().contextClassLoader.getResource(resName)
        ?: throw IllegalArgumentException("Failed to read resource \"${resName}\".")
}
