package com.github.dimguilpatcher.patcher

import com.github.dimguilpatcher.Config
import com.github.dimguilpatcher.util.Log
import java.io.File

class BinaryPatcherImpl(private val config: Config) : BinaryPatcher {
    private var source: MutableList<Byte> = mutableListOf()
    private var sourceName: String = ""
    private var bytesAdded: Int = 0

    override fun loadNewSource(file: String) {
        val f = File("${config.sourceBinariesPath}${File.separator}$file")
        if (!f.exists()) {
            throw IllegalArgumentException("Failed to load $file")
        }
        source = f.readBytes().toMutableList()
        sourceName = file
        bytesAdded = 0
    }

    override fun applyEdits(edits: Map<Address, List<Byte>>): BinaryPatcher {
        ensureSourceIsPresent()
        for (edit in edits.entries.sortedBy { it.key }) {
            val baseAddress = edit.key.toInt()
            val originalSize = source.size
            for ((i, element) in edit.value.withIndex()) {
                if (i + baseAddress >= originalSize) {
                    source += element
                    bytesAdded++
                } else {
                    source[i + baseAddress] = element
                }
            }
        }
        if (bytesAdded > 0) {
            Log.warn("$sourceName was expanded by $bytesAdded bytes")
        }
        return this
    }

    private fun ensureSourceIsPresent() {
        if (source.isEmpty()) {
            throw IllegalStateException("No file has been loaded.")
        }
    }

    override fun result(): ByteArray {
        ensureSourceIsPresent()
        return source.toByteArray()
    }
}