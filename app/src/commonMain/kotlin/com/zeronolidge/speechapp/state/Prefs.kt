package com.zeronolidge.speechapp.state

/**
 * A tiny key-value store, shaped like Android's SharedPreferences so the progress code reads
 * the same on both platforms. Android uses SharedPreferences; the web uses localStorage.
 */
interface Prefs {
    fun getInt(key: String, default: Int): Int
    fun getFloat(key: String, default: Float): Float
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getString(key: String, default: String?): String?
    fun getStringSet(key: String, default: Set<String>?): Set<String>?
    fun edit(): Editor

    interface Editor {
        fun putInt(key: String, value: Int): Editor
        fun putFloat(key: String, value: Float): Editor
        fun putBoolean(key: String, value: Boolean): Editor
        fun putString(key: String, value: String?): Editor
        fun putStringSet(key: String, value: Set<String>): Editor
        fun clear(): Editor
        fun apply()
    }
}
