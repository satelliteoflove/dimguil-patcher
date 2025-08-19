package com.github.dimguilpatcher

object Log {
    fun info(s: String) {
        println(s)
    }

    fun warn(s: String) {
        println("[WARNING] $s")
    }

    fun err(s: String) {
        System.err.println("[ERROR] $s")
    }
}