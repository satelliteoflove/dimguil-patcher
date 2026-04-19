package com.github.dimguilpatcher.textmanipulation

class NoCompression : CompressionStrategy {
    override fun compress(s: String): String {
        return s
    }
}