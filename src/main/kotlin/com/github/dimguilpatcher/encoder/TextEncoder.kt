package com.github.dimguilpatcher.encoder

import com.github.dimguilpatcher.TranslationUnit
import com.github.dimguilpatcher.WithEncodingTable
import com.github.dimguilpatcher.patcher.PatcherRule

interface TextEncoder : WithEncodingTable {
    fun encode(unit: TranslationUnit): PatcherRule
}