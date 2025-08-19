package com.github.dimguilpatcher

import kotlinx.serialization.Serializable

@Serializable
data class StringData(
    val headerAddress: UInt,
    val stringAddress: UInt,
    val length: UInt,
    val source: String,
    val translation: String = ""
)
