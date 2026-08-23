/*
 * Nocturne - by Mudassir

 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.db.entities

sealed class LocalItem {
    abstract val id: String
    abstract val title: String
    abstract val thumbnailUrl: String?
}
