package com.taqvo.app.widget

interface KeyValueStore {
    fun putString(key: String, value: String)
    fun getString(key: String): String?
}

expect fun createWidgetKeyValueStore(): KeyValueStore
