package com.github.dimguilpatcher

import kotlinx.serialization.Serializable

@Serializable
data class TranslationUnit(val file: String, val extendByBytes: Int? = null, val sections: List<SectionData>)
