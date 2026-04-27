package com.github.dimguilpatcher

import java.nio.file.Path

interface WithTable {
    fun parseTable(tablePath: Path)
}