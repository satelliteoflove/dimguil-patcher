package com.github.dimguilpatcher.textmanipulation

interface ControlCodeParser {
    fun parse(s: String, start: Int): List<Byte>
}