package com.github.dimguilpatcher.patcher

import kotlinx.serialization.Serializable

typealias Address = UInt

@Serializable(with = PatcherRuleSerializer::class)
data class PatcherRule(val file: String, val edits: Map<Address, List<Byte>>)
