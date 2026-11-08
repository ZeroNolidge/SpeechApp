package com.zeronolidge.speechapp.logic

import com.zeronolidge.speechapp.logic.VoiceStories.FruitPicker

/**
 * A hop into the bowl (grapes, strawberries) is applied only while the visit that counted it
 * is still the visit on screen. Leaving or starting again bumps the visit, so a delayed hop
 * from the previous play cannot drop pieces into the new bowl.
 */
object BowlLanding {
    fun countIfCurrent(scheduledVisit: Int, currentVisit: Int, count: Int): Int? =
        if (scheduledVisit == currentVisit) count else null
}

/** One id per Fruit Salad visit. Bumped when a play starts and when it is left. */
object FruitVisits {
    var id: Int = 0
        private set

    fun bump(): Int {
        id += 1
        return id
    }
}

/**
 * Choosing a fruit splices that fruit's own prep in next. Pieces count as in the bowl only
 * once that prep beat is showing its fix (or is already behind him). A new play starts from
 * the picker again, so the same fruit's prep runs again.
 */
object FruitPick {
    data class Result(val beats: List<VoiceBeat>, val picked: List<FruitPicker.Fruit>, val talks: Int)

    fun pick(
        beats: List<VoiceBeat>,
        index: Int,
        picked: List<FruitPicker.Fruit>,
        talks: Int,
        fruit: FruitPicker.Fruit,
    ): Result? {
        if (fruit in picked) return null
        var task = FruitPicker.task(fruit)
        var nextTalks = talks
        if (task.any { it.kitchen == null }) {
            if (nextTalks >= FruitPicker.MAX_FRUIT_TALKS) task = task.filter { it.kitchen != null } else nextTalks++
        }
        val nextPicked = picked + fruit
        val tail = task + if (nextPicked.size >= FruitPicker.Fruit.entries.size) listOf(FruitPicker.fork) else listOf(FruitPicker.picker)
        return Result(beats.take(index + 1) + tail, nextPicked, nextTalks)
    }

    /** True once [fruit]'s prep beat has played its fix, or is already behind the current beat. */
    fun inBowl(beats: List<VoiceBeat>, index: Int, phaseAct: Boolean, fruit: FruitPicker.Fruit): Boolean {
        val i = beats.indexOfFirst { it.fruitTag == fruit && it.kitchen != null }
        return i >= 0 && (index > i || (index == i && phaseAct))
    }
}
