package com.bozkurt.short3k

data class GameModel(
    val name: String,
    val code: String,
    val version: String = "1.00",
    val iconUri: String? = null,
    val backgroundUri: String? = null,
    var hasShortcut: Boolean = false
)
