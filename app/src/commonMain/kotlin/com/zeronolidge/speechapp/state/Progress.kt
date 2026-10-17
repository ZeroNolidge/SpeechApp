package com.zeronolidge.speechapp.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.zeronolidge.speechapp.logic.Ladder
import com.zeronolidge.speechapp.logic.TodayWords
import com.zeronolidge.speechapp.data.SpeechContent
import com.zeronolidge.speechapp.logic.Backup
import com.zeronolidge.speechapp.logic.Challenge
import com.zeronolidge.speechapp.logic.Catalog
import com.zeronolidge.speechapp.logic.Cosmetic
import com.zeronolidge.speechapp.logic.Land
import com.zeronolidge.speechapp.logic.Slot
import com.zeronolidge.speechapp.logic.Species
import com.zeronolidge.speechapp.logic.StoryParams
import com.zeronolidge.speechapp.logic.Trail
import com.zeronolidge.speechapp.logic.WordBook
import com.zeronolidge.speechapp.logic.WordStat
import com.zeronolidge.speechapp.ui.art.Outfit

/**
 * Everything the app remembers, saved on the device (SharedPreferences on Android,
 * localStorage on the web), so it survives closing the app.
 */
class Progress(private val prefs: Prefs) {

    // ---------- Stars (one per solved challenge; shown on the grown-ups page) ----------

    var stars by mutableIntStateOf(prefs.getInt("stars", 0))
        private set

    fun addStar(area: String) {
        stars += 1
        prefs.edit().putInt("stars", stars).putInt("area_$area", areaStars(area) + 1).apply()
    }

    fun areaStars(area: String) = prefs.getInt("area_$area", 0)

    fun level(key: String) = prefs.getInt("level_$key", 0)
    fun setLevel(key: String, value: Int) = prefs.edit().putInt("level_$key", value).apply()

    // ---------- The buddy ----------

    var hero by mutableStateOf(prefs.getString("hero", null)?.let { runCatching { Species.valueOf(it) }.getOrNull() })
        private set

    fun chooseHero(species: Species) {
        hero = species
        prefs.edit().putString("hero", species.name).apply()
    }

    private var owned by mutableStateOf(prefs.getStringSet("owned", emptySet())!!.toSet() + Catalog.starters)
    private var equipped by mutableStateOf(readEquipped())

    fun owns(c: Cosmetic) = c.id in owned

    /** Pieces the buddy has, in the order they were given out. */
    val pieces: List<Cosmetic> get() = (Catalog.starters + Catalog.awardOrder).filter { it in owned }.mapNotNull { Catalog.byId(it) }

    /**
     * Gives the buddy the next paper piece and puts it on. Returns null once the buddy has
     * every piece. Pieces are never taken away.
     */
    fun awardPiece(): Cosmetic? {
        val c = Catalog.nextPiece(owned) ?: return null
        owned = owned + c.id
        prefs.edit().putStringSet("owned", owned).apply()
        equip(c.slot, c)
        return c
    }

    fun equippedIn(slot: Slot): Cosmetic? = Catalog.byId(equipped[slot])

    fun equip(slot: Slot, c: Cosmetic?) {
        equipped = equipped + (slot to c?.id)
        prefs.edit().putString("equip_${slot.name}", c?.id).apply()
    }

    private fun readEquipped(): Map<Slot, String?> = Slot.entries.associateWith { prefs.getString("equip_${it.name}", null) }

    val outfit: Outfit get() = Outfit(equippedIn(Slot.HAT), equippedIn(Slot.CAPE), equippedIn(Slot.GLASSES), equippedIn(Slot.WINGS))

    val pet: Species? get() = equippedIn(Slot.PET)?.pet

    fun heroColor(species: Species = hero ?: Species.KID): Color {
        val chosen = equippedIn(Slot.COLOR)?.color?.takeIf { it != 0L }
        return Color(chosen ?: if (species == Species.KID) 0xFF5B7FDB else species.defaultColor)
    }

    // ---------- The backpack ----------

    /** Gems in the backpack right now (shown as pictures, never as a number). */
    var backpack by mutableIntStateOf(prefs.getInt("backpack", 0))
        private set

    /**
     * One gem for trying to say a word. When the backpack is full, it empties and the buddy gets
     * a new paper piece, which is returned (null otherwise).
     */
    fun addGem(): Cosmetic? {
        backpack++
        var piece: Cosmetic? = null
        if (backpack >= BACKPACK_SIZE) {
            backpack = 0
            piece = awardPiece()
        }
        prefs.edit().putInt("backpack", backpack).apply()
        return piece
    }

    // ---------- The trail ----------

    /** Where the child is on the trail: the number of the next chapter to play. */
    var chapter by mutableIntStateOf(prefs.getInt("chapter", 0))
        private set

    /**
     * Chapters actually finished, for the grown-ups page. Counted separately from [chapter]: an
     * earlier version started the trail from the old "quests" count, so quests played before the
     * trail existed are taken off for anyone who updated.
     */
    var chaptersDone by mutableIntStateOf(prefs.getInt("chaptersDone", (chapter - prefs.getInt("quests", 0)).coerceAtLeast(0)))
        private set

    /** Tags on Pip's collar: one for each time he found Lost Puppy Pip. */
    var pipTags by mutableIntStateOf(prefs.getInt("pipTags", 0))
        private set

    fun addPipTag() {
        pipTags++
        prefs.edit().putInt("pipTags", pipTags).apply()
    }

    /** Voice stories he has played to the end (their map stops wear a star). */
    var voiceStoriesDone by mutableStateOf(prefs.getStringSet("voiceStoriesDone", emptySet()) ?: emptySet())
        private set

    fun finishVoiceStory(id: String) {
        voiceStoriesDone = voiceStoriesDone + id
        prefs.edit().putStringSet("voiceStoriesDone", voiceStoriesDone).apply()
    }

    /** The last chapter whose stop has already been shown unfolding on the map. */
    var unfoldedSeen by mutableIntStateOf(prefs.getInt("unfoldedSeen", 0))
        private set

    fun finishChapter() {
        chapter++
        chaptersDone++
        prefs.edit().putInt("chapter", chapter).putInt("chaptersDone", chaptersDone).apply()
    }

    fun markUnfoldSeen() {
        unfoldedSeen = chapter
        prefs.edit().putInt("unfoldedSeen", chapter).apply()
    }

    val currentLand: Land get() = Trail.landOf(chapter)

    /**
     * Called when the map opens. On the first visit of a new day (after the very first day),
     * the buddy gets a "welcome back" piece for showing up. Returns that piece, if any.
     */
    /** Days since the last visit before today (0 on the first visit or the same day). */
    var daysAway = 0
        private set

    fun checkIn(day: Int): Cosmetic? {
        val last = prefs.getInt("lastDay", 0)
        if (last == day) return null
        daysAway = if (last == 0) 0 else day - last
        prefs.edit().putInt("lastDay", day).apply()
        return if (last == 0) null else awardPiece()
    }

    // ---------- Words and stickers ----------

    var words by mutableStateOf(WordBook.decode(prefs.getStringSet("words", emptySet())!!))
        private set

    /** Records an honest try at a word; it becomes a sticker (a shiny one if heard clearly). */
    fun recordWord(text: String, emoji: String, clear: Boolean, heard: Boolean): WordStat {
        words = words.record(text, emoji, today(), clear, heard)
        prefs.edit().putStringSet("words", words.encode()).apply()
        return words[text]!!
    }

    /** How hard the speech words are: 0 = one syllable, 1 = two syllables, 2 = short phrases. */
    var speechStep by mutableIntStateOf(prefs.getInt("speechStep", 0))
        private set

    /** Clear catches in a row, used to step the words up quietly. */
    private var clearRun = prefs.getInt("clearRun", 0)
    private var missRun = prefs.getInt("speechMissRun", 0)

    /**
     * Quietly adjusts how hard the words are (one word, two words, a short sentence): see
     * [Ladder]. The child is never told.
     */
    fun speechResult(clear: Boolean) {
        speechStep = ladderResult(Ladder.SPEECH, clear).step
        prefs.edit().putInt("speechStep", speechStep).apply()
    }

    // ---------- Quiet difficulty ----------

    /** Counts app starts, so "across two sessions" can be checked. */
    val sessionNo: Int = prefs.getInt("sessionNo", 0) + 1

    /** The current sitting of play (see [com.zeronolidge.speechapp.logic.PlaySession]), kept so a reload carries on. */
    var playSession: com.zeronolidge.speechapp.logic.PlaySession?
        get() = com.zeronolidge.speechapp.logic.PlaySession.decode(prefs.getString("playSession", ""))
        set(v) { prefs.edit().putString("playSession", v?.encode() ?: "").apply() }

    init {
        prefs.edit().putInt("sessionNo", sessionNo).apply()
    }

    fun ladder(key: String): Ladder =
        Ladder.decode(prefs.getString("ladder_$key", null)).let { if (key == Ladder.SPEECH && it.step == 0 && it.recent.isEmpty()) Ladder(speechStep) else it }

    /** Records a right (or not right first time) answer on one ladder and saves it. */
    fun ladderResult(key: String, right: Boolean): Ladder {
        val next = ladder(key).record(right, sessionNo, Ladder.MAX[key] ?: 2)
        prefs.edit().putString("ladder_$key", next.encode()).apply()
        return next
    }

    /** A puzzle's result on its ladder: right means right first time (numbers and patterns). */
    fun quizResult(c: Challenge, right: Boolean) {
        when (c) {
            is Challenge.Count, is Challenge.Math, is Challenge.More -> ladderResult(Ladder.NUMBERS, right)
            is Challenge.Pattern, is Challenge.OddOne -> ladderResult(Ladder.PATTERNS, right)
            else -> {}
        }
    }

    val numbersStep: Int get() = ladder(Ladder.NUMBERS).step
    val patternStep: Int get() = ladder(Ladder.PATTERNS).step

    // ---------- Echo Cave, the reward stop after finding Pip ----------

    var echoReady by mutableStateOf(prefs.getBoolean("echoReady", false))
        private set

    fun updateEchoReady(ready: Boolean) {
        echoReady = ready
        prefs.edit().putBoolean("echoReady", ready).apply()
    }

    /** 2 or 3 words for a grown-up to say with him today (grown-ups page). */
    fun wordsToSayToday(): List<String> {
        val target = focusSounds.mapNotNull { id -> SpeechContent.sounds.firstOrNull { it.id == id } }
            .flatMap { s -> s.words.take(2).map { it.text } }
        return TodayWords.pick(words, today(), customWords.map { it.first } + target)
    }

    /** A grown-up's own words for their child, like family names. Each is "word|emoji". */
    var customWords by mutableStateOf(readCustom())
        private set

    private fun readCustom(): List<Pair<String, String>> = prefs.getString("customWords", "")!!
        .split("\n").filter { "|" in it }.map { it.substringBefore("|") to it.substringAfter("|") }

    fun addCustomWord(text: String, emoji: String) {
        val t = text.trim()
        if (t.isEmpty() || customWords.size >= MAX_CUSTOM || customWords.any { it.first.equals(t, true) }) return
        customWords = customWords + (t to emoji)
        saveCustom()
    }

    fun removeCustomWord(text: String) {
        customWords = customWords.filter { it.first != text }
        saveCustom()
    }

    private fun saveCustom() = prefs.edit().putString("customWords", customWords.joinToString("\n") { "${it.first}|${it.second}" }).apply()

    // ---------- Grown-up settings ----------

    var childName by mutableStateOf(prefs.getString("childName", "") ?: "")
        private set

    var speechRate by mutableFloatStateOf(prefs.getFloat("speechRate", 0.85f))
        private set

    var useMic by mutableStateOf(prefs.getBoolean("useMic", true))
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

    /** The "Can Rory hear you?" screen has been shown (it appears once, on first launch). */
    var micAsked by mutableStateOf(prefs.getBoolean("micAsked", false))
        private set

    fun markMicAsked() {
        micAsked = true
        prefs.edit().putBoolean("micAsked", true).apply()
    }

    fun toggleFocusSound(id: String) {
        // One or two target sounds: picking a third swaps out the earliest pick.
        focusSounds = when {
            id in focusSounds -> focusSounds - id
            focusSounds.size >= MAX_FOCUS -> focusSounds.drop(1).toSet() + id
            else -> focusSounds + id
        }
        prefs.edit().putStringSet("focus", focusSounds).apply()
    }

    fun storyParams() = StoryParams(
        heroName = heroName,
        chapter = chapter,
        focusSounds = focusSounds,
        robotSolved = level("story_robot"),
        patternLevel = patternStep,
        numbersStep = numbersStep,
        numbersSolved = areaStars(AREA_NUMBERS),
        lettersSolved = areaStars(AREA_LETTERS),
        reachedLands = Trail.reached(chapter),
        speechStep = speechStep,
        today = today(),
        words = words,
        customWords = customWords,
    )

    // ---------- Backup ----------

    private val intKeys get() = listOf("stars", "chapter", "chaptersDone", "unfoldedSeen", "lastDay", "speechStep", "clearRun", "speechMissRun", "backpack", "pipTags") +
        areas.map { "area_$it" } + listOf("patterns", "story_robot", "robot_easy", "robot_turn").map { "level_$it" }
    private val stringKeys get() = listOf("hero", "childName", "customWords") + Slot.entries.map { "equip_${it.name}" } +
        Ladder.MAX.keys.map { "ladder_$it" }
    private val setKeys = listOf("owned", "words", "focus")

    /** Everything worth keeping, as a code a grown-up can copy and paste on another device. */
    fun exportCode(): String {
        val m = mutableMapOf<String, String>()
        intKeys.forEach { m[it] = "i:" + prefs.getInt(it, 0) }
        m["speechRate"] = "f:" + prefs.getFloat("speechRate", 0.85f)
        m["useMic"] = "b:" + prefs.getBoolean("useMic", true)
        stringKeys.forEach { k -> prefs.getString(k, null)?.let { m[k] = "s:$it" } }
        setKeys.forEach { k -> m[k] = "S:" + prefs.getStringSet(k, emptySet())!!.joinToString(SET_SEP) }
        return Backup.encode(m)
    }

    /** Replaces all progress with the one in [code]. Returns false (and changes nothing) if the code isn't valid. */
    fun importCode(code: String): Boolean {
        val m = Backup.decode(code) ?: return false
        if ("chapter" !in m) return false
        val e = prefs.edit().clear()
        m.forEach { (k, v) ->
            val body = v.drop(2)
            when (v.take(2)) {
                "i:" -> body.toIntOrNull()?.let { e.putInt(k, it) }
                "f:" -> body.toFloatOrNull()?.let { e.putFloat(k, it) }
                "b:" -> e.putBoolean(k, body == "true")
                "s:" -> e.putString(k, body)
                "S:" -> e.putStringSet(k, if (body.isEmpty()) emptySet() else body.split(SET_SEP).toSet())
            }
        }
        e.apply()
        load()
        return true
    }

    /** Reads everything back from storage (after a restore). */
    private fun load() {
        stars = prefs.getInt("stars", 0)
        hero = prefs.getString("hero", null)?.let { runCatching { Species.valueOf(it) }.getOrNull() }
        owned = prefs.getStringSet("owned", emptySet())!!.toSet() + Catalog.starters
        equipped = readEquipped()
        backpack = prefs.getInt("backpack", 0)
        chapter = prefs.getInt("chapter", 0)
        pipTags = prefs.getInt("pipTags", 0)
        chaptersDone = prefs.getInt("chaptersDone", 0)
        unfoldedSeen = prefs.getInt("unfoldedSeen", 0)
        words = WordBook.decode(prefs.getStringSet("words", emptySet())!!)
        speechStep = prefs.getInt("speechStep", 0)
        clearRun = prefs.getInt("clearRun", 0)
        missRun = prefs.getInt("speechMissRun", 0)
        customWords = readCustom()
        childName = prefs.getString("childName", "") ?: ""
        speechRate = prefs.getFloat("speechRate", 0.85f)
        useMic = prefs.getBoolean("useMic", true)
        focusSounds = prefs.getStringSet("focus", emptySet())!!.toSet()
    }

    /** Clears the buddy, the trail, pieces, stickers and stars. Grown-up settings and own words are kept. */
    fun reset() {
        prefs.edit().clear()
            .putFloat("speechRate", speechRate).putBoolean("useMic", useMic)
            .putString("childName", childName)
            .putStringSet("focus", focusSounds)
            .putString("customWords", customWords.joinToString("\n") { "${it.first}|${it.second}" })
            .apply()
        stars = 0
        hero = null
        owned = Catalog.starters
        equipped = readEquipped()
        chapter = 0
        chaptersDone = 0
        pipTags = 0
        unfoldedSeen = 0
        words = WordBook()
        speechStep = 0
        clearRun = 0
        missRun = 0
        backpack = 0
    }

    companion object {
        const val AREA_SPEECH = "speech"
        const val AREA_LETTERS = "letters"
        const val AREA_READING = "reading"
        const val AREA_NUMBERS = "numbers"
        const val AREA_COINS = "coins"
        const val AREA_PATTERNS = "patterns"
        const val AREA_ROBOT = "robot"
        val areas = listOf(AREA_SPEECH, AREA_LETTERS, AREA_READING, AREA_NUMBERS, AREA_COINS, AREA_PATTERNS, AREA_ROBOT)
        const val MAX_SPEECH_STEP = 2
        const val MAX_CUSTOM = 8
        const val MAX_FOCUS = 2
        const val BACKPACK_SIZE = 4
        private const val SET_SEP = "\u001F"
    }
}
