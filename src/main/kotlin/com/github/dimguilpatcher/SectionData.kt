package com.github.dimguilpatcher

import kotlinx.serialization.Serializable

@Serializable
data class SectionData(
    val firstHeaderAddress: ULong,
    val firstHeader: UShort,
    val sectionLength: UInt,
    val compress: Boolean? = false,
    val strings: List<StringData>
)