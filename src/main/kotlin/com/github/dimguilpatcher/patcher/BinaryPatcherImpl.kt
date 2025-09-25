package com.github.dimguilpatcher.patcher

import com.github.dimguilpatcher.Config
import java.io.File

class BinaryPatcherImpl(private val config: Config) : BinaryPatcher {
    private var source: ByteArray = byteArrayOf()

    override fun loadNewSource(file: String) {
        val f = File("${config.sourceBinariesPath}${File.separator}$file")
        if (!f.exists()) {
            throw IllegalArgumentException("Failed to load $file")
        }
        source = f.readBytes()
    }

    override fun applyEdits(edits: Map<Address, List<Byte>>): BinaryPatcher {
        checkSourceIsPresent()
        for (edit in edits) {
            val baseAddress = edit.key.toInt()
            for (i in 0..<edit.value.count()) {
                source[i + baseAddress] = edit.value[i]
            }
        }
        return this
    }

    private fun checkSourceIsPresent() {
        if (source.isEmpty()) {
            throw IllegalStateException("No file has been loaded.")
        }
    }

    override fun result(): ByteArray {
        checkSourceIsPresent()
        return source.copyOf()
    }
}