/*
 * Nocturne - by Mudassir

 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.extensions

fun <T> tryOrNull(block: () -> T): T? =
    try {
        block()
    } catch (e: Exception) {
        null
    }
