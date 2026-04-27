package com.github.dimguilpatcher.decoder

import com.github.dimguilpatcher.SectionConfigData
import com.github.dimguilpatcher.TranslationUnit
import com.github.dimguilpatcher.WithTable

interface TextDecoder : WithTable {
    fun decode(metadata: List<SectionConfigData>): List<TranslationUnit>
}