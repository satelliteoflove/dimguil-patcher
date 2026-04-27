package com.github.dimguilpatcher.encoder

import com.github.dimguilpatcher.TranslationUnit
import com.github.dimguilpatcher.WithTable
import com.github.dimguilpatcher.patcher.PatcherRule

interface TextEncoder : WithTable {
    fun encodeUnit(unit: TranslationUnit): PatcherRule

    fun encodePlainString(s: String, compress: Boolean): List<Byte>
}