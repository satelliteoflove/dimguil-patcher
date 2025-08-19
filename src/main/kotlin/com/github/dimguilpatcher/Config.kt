package com.github.dimguilpatcher

import kotlinx.serialization.Serializable

@Serializable
data class Config(val gameBinariesPath: String)
