package com.github.dimguilpatcher

import kotlinx.serialization.Serializable

@Serializable
data class SectionData(val firstHeaderAddress: UShort, val firstHeader: UShort, val sectionLength: UInt, val strings: List<StringData>)