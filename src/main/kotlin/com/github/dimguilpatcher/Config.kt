package com.github.dimguilpatcher

import kotlinx.serialization.Serializable

@Serializable
data class Config(val sourceBinariesPath: String, val targetBinariesPath: String)
