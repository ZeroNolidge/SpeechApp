package com.zeronolidge.speechapp.state

import android.content.Context
import android.content.SharedPreferences

/** [Prefs] backed by SharedPreferences, so existing saved progress carries over. */
class AndroidPrefs(context: Context) : Prefs {
    private val sp: SharedPreferences = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    override fun getInt(key: String, default: Int) = sp.getInt(key, default)
    override fun getFloat(key: String, default: Float) = sp.getFloat(key, default)
    override fun getBoolean(key: String, default: Boolean) = sp.getBoolean(key, default)
    override fun getString(key: String, default: String?): String? = sp.getString(key, default)
    override fun getStringSet(key: String, default: Set<String>?): Set<String>? = sp.getStringSet(key, default)

    override fun edit(): Prefs.Editor = object : Prefs.Editor {
        private val e = sp.edit()
        override fun putInt(key: String, value: Int): Prefs.Editor = also { e.putInt(key, value) }
        override fun putFloat(key: String, value: Float): Prefs.Editor = also { e.putFloat(key, value) }
        override fun putBoolean(key: String, value: Boolean): Prefs.Editor = also { e.putBoolean(key, value) }
        override fun putString(key: String, value: String?): Prefs.Editor = also { e.putString(key, value) }
        override fun putStringSet(key: String, value: Set<String>): Prefs.Editor = also { e.putStringSet(key, value) }
        override fun clear(): Prefs.Editor = also { e.clear() }
        override fun apply() = e.apply()
    }
}
