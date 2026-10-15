package com.zeronolidge.speechapp.logic

/**
 * A backup code for the child's progress, so a cleared browser or a new tablet doesn't start
 * over. It's plain text a grown-up can copy, email to themselves and paste back.
 *
 * Format: "TALK1-" + base64 of "key=value" lines, + "-" + a short checksum, so a mistyped or
 * cut-off code is rejected instead of half-loaded.
 */
object Backup {
    private const val PREFIX = "TALK1-"

    fun encode(values: Map<String, String>): String {
        val body = values.entries.sortedBy { it.key }.joinToString("\n") { (k, v) -> "$k=${escape(v)}" }
        val b64 = Base64Lite.encode(body.encodeToByteArray())
        return PREFIX + b64 + "-" + checksum(b64)
    }

    /** The values in [code], or null if it isn't a valid backup code. */
    fun decode(code: String): Map<String, String>? {
        val c = code.filterNot { it.isWhitespace() }
        if (!c.startsWith(PREFIX)) return null
        val rest = c.removePrefix(PREFIX)
        val dash = rest.lastIndexOf('-')
        if (dash < 0) return null
        val b64 = rest.substring(0, dash)
        if (rest.substring(dash + 1) != checksum(b64)) return null
        val body = Base64Lite.decode(b64)?.decodeToString() ?: return null
        if (body.isEmpty()) return emptyMap()
        return body.split("\n").filter { '=' in it }.associate { it.substringBefore('=') to unescape(it.substringAfter('=')) }
    }

    private fun escape(v: String) = v.replace("\\", "\\\\").replace("\n", "\\n")

    private fun unescape(v: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < v.length) {
            val ch = v[i]
            if (ch == '\\' && i + 1 < v.length) {
                out.append(if (v[i + 1] == 'n') '\n' else v[i + 1])
                i += 2
            } else {
                out.append(ch)
                i++
            }
        }
        return out.toString()
    }

    private fun checksum(s: String): String {
        var h = 7
        for (ch in s) h = h * 31 + ch.code
        return (h.toUInt() % 46656u).toString(36).padStart(3, '0')
    }
}

/** A small base64 (standard alphabet, with padding), so the code is the same on every platform. */
internal object Base64Lite {
    private const val ABC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun encode(data: ByteArray): String {
        val sb = StringBuilder()
        var i = 0
        while (i < data.size) {
            val b0 = data[i].toInt() and 0xFF
            val b1 = if (i + 1 < data.size) data[i + 1].toInt() and 0xFF else -1
            val b2 = if (i + 2 < data.size) data[i + 2].toInt() and 0xFF else -1
            sb.append(ABC[b0 shr 2])
            sb.append(ABC[((b0 and 3) shl 4) or (if (b1 < 0) 0 else b1 shr 4)])
            sb.append(if (b1 < 0) '=' else ABC[((b1 and 15) shl 2) or (if (b2 < 0) 0 else b2 shr 6)])
            sb.append(if (b2 < 0) '=' else ABC[b2 and 63])
            i += 3
        }
        return sb.toString()
    }

    fun decode(s: String): ByteArray? {
        if (s.length % 4 != 0) return null
        val out = ArrayList<Byte>()
        var i = 0
        while (i < s.length) {
            val v = IntArray(4)
            for (k in 0 until 4) {
                val ch = s[i + k]
                v[k] = if (ch == '=') -1 else ABC.indexOf(ch).also { if (it < 0) return null }
            }
            if (v[0] < 0 || v[1] < 0) return null
            out.add(((v[0] shl 2) or (v[1] shr 4)).toByte())
            if (v[2] >= 0) out.add((((v[1] and 15) shl 4) or (v[2] shr 2)).toByte())
            if (v[2] >= 0 && v[3] >= 0) out.add((((v[2] and 3) shl 6) or v[3]).toByte())
            i += 4
        }
        return out.toByteArray()
    }
}
