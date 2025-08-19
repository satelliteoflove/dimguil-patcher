package com.github.dimguilpatcher.decoder

import com.github.dimguilpatcher.TranslationUnit
import com.github.dimguilpatcher.SectionConfigData
import com.github.dimguilpatcher.WithEncodingTable

interface TextDecoder : WithEncodingTable {
    fun decode(metadata: List<SectionConfigData>): List<TranslationUnit>
}