package com.github.dimguilpatcher.patcher

interface BinaryPatcher {
    fun loadNewSource(file: String)

    fun applyEdits(edits: Map<Address, List<Byte>>): BinaryPatcher

    fun result(): ByteArray
}
