package com.github.dimguilpatcher

import kotlinx.serialization.Serializable

@Serializable
data class StringData(
    val length: UInt,
    val source: String,
    val translation: String = "",
    val addStringTerminator: Boolean? = true
)
