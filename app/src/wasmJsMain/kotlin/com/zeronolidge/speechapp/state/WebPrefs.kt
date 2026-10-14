package com.zeronolidge.speechapp.state

import kotlin.js.js

private fun lsGet(k: String): String? = js("(() => { try { return window.localStorage.getItem(k); } catch (e) { return null; } })()")
private fun lsSet(k: String, v: String): Unit = js("(() => { try { window.localStorage.setItem(k, v); } catch (e) {} })()")
private fun lsRemove(k: String): Unit = js("(() => { try { window.localStorage.removeItem(k); } catch (e) {} })()")
private fun lsClear(prefix: String): Unit = js(
    """(() => { try {
        const ks = [];
        for (let i = 0; i < localStorage.length; i++) { const k = localStorage.key(i); if (k && k.startsWith(prefix)) ks.push(k); }
        ks.forEach(k => localStorage.removeItem(k));
    } catch (e) {} })()"""
)

/** [Prefs] backed by the browser's localStorage, so progress survives closing the tab. */
class WebPrefs(private val prefix: String = "talkandlearn.") : Prefs {
    private val sep = '\u001F'

    private fun get(key: String) = lsGet(prefix + key)

    override fun getInt(key: String, default: Int) = get(key)?.toIntOrNull() ?: default
    override fun getFloat(key: String, default: Float) = get(key)?.toFloatOrNull() ?: default
    override fun getBoolean(key: String, default: Boolean) = get(key)?.toBooleanStrictOrNull() ?: default
    override fun getString(key: String, default: String?) = get(key) ?: default
    override fun getStringSet(key: String, default: Set<String>?): Set<String>? =
        get(key)?.let { s -> if (s.isEmpty()) emptySet() else s.split(sep).toSet() } ?: default

    override fun edit(): Prefs.Editor = object : Prefs.Editor {
        private val ops = mutableListOf<() -> Unit>()
        private fun put(key: String, value: String?) = also {
            ops += { if (value == null) lsRemove(prefix + key) else lsSet(prefix + key, value) }
        }
        override fun putInt(key: String, value: Int): Prefs.Editor = put(key, value.toString())
        override fun putFloat(key: String, value: Float): Prefs.Editor = put(key, value.toString())
        override fun putBoolean(key: String, value: Boolean): Prefs.Editor = put(key, value.toString())
        override fun putString(key: String, value: String?): Prefs.Editor = put(key, value)
        override fun putStringSet(key: String, value: Set<String>): Prefs.Editor = put(key, value.joinToString(sep.toString()))
        override fun clear(): Prefs.Editor = also { ops += { lsClear(prefix) } }
        override fun apply() = ops.forEach { it() }
    }
}
