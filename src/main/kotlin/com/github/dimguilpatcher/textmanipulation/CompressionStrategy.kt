package com.github.dimguilpatcher.textmanipulation

interface CompressionStrategy {
    fun compress(s: String): String
}