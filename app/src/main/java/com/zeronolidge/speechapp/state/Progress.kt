package com.zeronolidge.speechapp.state

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Stars earned, levels reached and grown-up settings, saved on the device. */
class Progress(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    var stars by mutableIntStateOf(prefs.getInt("stars", 0))
        private set

    var speechRate by mutableFloatStateOf(prefs.getFloat("speechRate", 0.85f))
        private set

    var useMic by mutableStateOf(prefs.getBoolean("useMic", true))
        private set

    fun addStar(area: String) {
        stars += 1
        prefs.edit().putInt("stars", stars).putInt("area_$area", areaStars(area) + 1).apply()
    }

    fun areaStars(area: String) = prefs.getInt("area_$area", 0)

    fun level(key: String) = prefs.getInt("level_$key", 0)
    fun setLevel(key: String, value: Int) = prefs.edit().putInt("level_$key", value).apply()

    fun updateSpeechRate(v: Float) {
        speechRate = v
        prefs.edit().putFloat("speechRate", v).apply()
    }

    fun updateUseMic(v: Boolean) {
        useMic = v
        prefs.edit().putBoolean("useMic", v).apply()
    }

    fun reset() {
        val rate = speechRate
        val mic = useMic
        prefs.edit().clear().putFloat("speechRate", rate).putBoolean("useMic", mic).apply()
        stars = 0
    }

    companion object {
        const val AREA_SPEECH = "speech"
        const val AREA_LETTERS = "letters"
        const val AREA_READING = "reading"
        const val AREA_NUMBERS = "numbers"
        const val AREA_PATTERNS = "patterns"
        const val AREA_ROBOT = "robot"
        val areas = listOf(AREA_SPEECH, AREA_LETTERS, AREA_READING, AREA_NUMBERS, AREA_PATTERNS, AREA_ROBOT)
    }
}
