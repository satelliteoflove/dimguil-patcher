package com.github.dimguilpatcher

import com.github.dimguilpatcher.json.HeaderDataSerializer
import kotlinx.serialization.Serializable

@Serializable
data class SectionConfigData(val file: String, val headers: List<HeaderData>)

@Serializable(with = HeaderDataSerializer::class)
data class HeaderData(val address: ULong)
