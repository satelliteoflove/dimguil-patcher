package com.github.dimguilpatcher.patcher

import com.github.dimguilpatcher.util.Log
import java.nio.file.Path
import kotlin.io.path.notExists
import kotlin.io.path.readBytes

class BinaryPatcherImpl(private val cleanSourcePath: Path) : BinaryPatcher {
    private var source: MutableList<Byte> = mutableListOf()
    private var sourceName: String = ""
    private var bytesAdded: Int = 0

    override fun loadNewSource(file: String) {
        val f = cleanSourcePath.resolve(file)
        if (f.notExists()) {
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