package com.zeronolidge.speechapp.state

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.zeronolidge.speechapp.logic.Catalog
import com.zeronolidge.speechapp.logic.Cosmetic
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.logic.Leveling
import com.zeronolidge.speechapp.logic.Slot
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.logic.StoryParams
import com.zeronolidge.speechapp.ui.art.Outfit

/** Stars, the hero, levels and grown-up settings, saved on the device. */
class Progress(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    // ---------- Stars (used by free practice) ----------

    var stars by mutableIntStateOf(prefs.getInt("stars", 0))
        private set

    fun addStar(area: String) {
        stars += 1
        prefs.edit().putInt("stars", stars).putInt("area_$area", areaStars(area) + 1).apply()
    }

    fun areaStars(area: String) = prefs.getInt("area_$area", 0)

    fun level(key: String) = prefs.getInt("level_$key", 0)
    fun setLevel(key: String, value: Int) = prefs.edit().putInt("level_$key", value).apply()

    // ---------- The hero ----------

    var hero by mutableStateOf(prefs.getString("hero", null)?.let { runCatching { Species.valueOf(it) }.getOrNull() })
        private set

    var xp by mutableIntStateOf(prefs.getInt("xp", 0))
        private set

    var gems by mutableIntStateOf(prefs.getInt("gems", 0))
        private set

    var questsDone by mutableIntStateOf(prefs.getInt("quests", 0))
        private set

    private var owned by mutableStateOf(prefs.getStringSet("owned", emptySet())!!.toSet())
    private var equipped by mutableStateOf(readEquipped())

    val heroLevel: Int get() = Leveling.levelFor(xp)

    fun chooseHero(species: Species) {
        hero = species
        prefs.edit().putString("hero", species.name).apply()
    }

    /** Adds XP and gems; returns the new level if the hero levelled up, otherwise null. */
    fun gain(xpGained: Int, gemsGained: Int): Int? {
        val before = heroLevel
        xp += xpGained
        gems += gemsGained
        prefs.edit().putInt("xp", xp).putInt("gems", gems).apply()
        return heroLevel.takeIf { it > before }
    }

    fun finishQuest() {
        questsDone++
        prefs.edit().putInt("quests", questsDone).apply()
    }

    fun owns(c: Cosmetic) = c.id in owned || (c.price == 0 && heroLevel >= c.level)

    /** Spends gems on an item. Returns false if it isn't available yet or is too expensive. */
    fun buy(c: Cosmetic): Boolean {
        if (owns(c) || heroLevel < c.level || gems < c.price) return false
        gems -= c.price
        owned = owned + c.id
        prefs.edit().putInt("gems", gems).putStringSet("owned", owned).apply()
        return true
    }

    fun equippedIn(slot: Slot): Cosmetic? = Catalog.byId(equipped[slot])

    fun equip(slot: Slot, c: Cosmetic?) {
        equipped = equipped + (slot to c?.id)
        prefs.edit().putString("equip_${slot.name}", c?.id).apply()
    }

    private fun readEquipped(): Map<Slot, String?> = Slot.entries.associateWith { prefs.getString("equip_${it.name}", null) }

    val outfit: Outfit get() = Outfit(equippedIn(Slot.HAT), equippedIn(Slot.CAPE), equippedIn(Slot.GLASSES))

    val pet: Species? get() = equippedIn(Slot.PET)?.pet

    fun heroColor(species: Species = hero ?: Species.KID): Color {
        val chosen = equippedIn(Slot.COLOR)?.color?.takeIf { it != 0L }
        return Color(chosen ?: if (species == Species.KID) 0xFFFF7043 else species.defaultColor)
    }

    // ---------- Grown-up settings ----------

    var childName by mutableStateOf(prefs.getString("childName", "") ?: "")
        private set

    var speechRate by mutableFloatStateOf(prefs.getFloat("speechRate", 0.85f))
        private set

    var useMic by mutableStateOf(prefs.getBoolean("useMic", true))
        private set

    var openAllLands by mutableStateOf(prefs.getBoolean("openAll", false))
        private set

    var focusSounds by mutableStateOf(prefs.getStringSet("focus", emptySet())!!.toSet())
        private set

    /** What the story calls the child. */
    val heroName: String get() = childName.ifBlank { hero?.label ?: "Hero" }

    fun updateChildName(v: String) {
        childName = v
        prefs.edit().putString("childName", v.trim()).apply()
    }

    fun updateSpeechRate(v: Float) {
        speechRate = v
        prefs.edit().putFloat("speechRate", v).apply()
    }

    fun updateUseMic(v: Boolean) {
        useMic = v
        prefs.edit().putBoolean("useMic", v).apply()
    }

    fun updateOpenAllLands(v: Boolean) {
        openAllLands = v
        prefs.edit().putBoolean("openAll", v).apply()
    }

    fun toggleFocusSound(id: String) {
        focusSounds = if (id in focusSounds) focusSounds - id else focusSounds + id
        prefs.edit().putStringSet("focus", focusSounds).apply()
    }

    fun landOpen(land: Land) = land.unlocked(heroLevel, openAllLands)

    private var visited by mutableStateOf(
        prefs.getStringSet("visited", emptySet())!!.mapNotNull { runCatching { Land.valueOf(it) }.getOrNull() }.toSet(),
    )
    private var lastLand by mutableStateOf(prefs.getString("lastLand", null)?.let { runCatching { Land.valueOf(it) }.getOrNull() })

    fun visit(land: Land) {
        visited = visited + land
        lastLand = land
        prefs.edit().putStringSet("visited", visited.map { it.name }.toSet()).putString("lastLand", land.name).apply()
    }

    /** The land the map points at next. */
    fun suggestedLand(): Land = Land.suggest(Land.entries.filter { landOpen(it) }, visited, lastLand)

    fun storyParams() = StoryParams(
        heroName = heroName,
        heroLevel = heroLevel,
        focusSounds = focusSounds,
        robotSolved = level("story_robot"),
        patternLevel = level("patterns"),
        numbersSolved = areaStars(AREA_NUMBERS),
        lettersSolved = areaStars(AREA_LETTERS),
        unlockedLands = Land.entries.filter { landOpen(it) && it != Land.CLOUDS },
    )

    /** Clears stars, the hero and all unlocks. Grown-up settings are kept. */
    fun reset() {
        prefs.edit().clear()
            .putFloat("speechRate", speechRate).putBoolean("useMic", useMic)
            .putBoolean("openAll", openAllLands).putString("childName", childName)
            .putStringSet("focus", focusSounds)
            .apply()
        stars = 0
        xp = 0
        gems = 0
        questsDone = 0
        hero = null
        owned = emptySet()
        equipped = readEquipped()
        visited = emptySet()
        lastLand = null
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
