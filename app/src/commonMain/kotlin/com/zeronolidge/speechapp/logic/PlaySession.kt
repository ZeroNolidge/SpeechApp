package com.zeronolidge.speechapp.logic

/**
 * One sitting of play, about [MINUTES] long, kept on the device so a reload (or the tablet going
 * to sleep for a moment) doesn't start it again. Away longer than [GAP_MINUTES] is a new sitting.
 * After [MINUTES], Rory wraps up at the next natural pause ("Pip's sleepy, let's rest!"), once.
 * Times are wall-clock milliseconds.
 */
data class PlaySession(val start: Long, val lastSeen: Long, val rested: Boolean = false) {
    /** Long enough: time for Rory to wrap up at the next pause (unless he already has). */
    fun long(now: Long): Boolean = !rested && now - start >= MINUTES * MINUTE

    /** Still playing at [now]: the same sitting, just seen again. */
    fun seen(now: Long): PlaySession = copy(lastSeen = maxOf(lastSeen, now))

    fun encode(): String = "$start,$lastSeen,${if (rested) 1 else 0}"

    companion object {
        const val MINUTES = 12
        const val GAP_MINUTES = 30
        private const val MINUTE = 60_000L

        /** The sitting to carry on at [now]: the saved one, unless it's gone quiet for a while (or the clock went back). */
        fun resume(saved: PlaySession?, now: Long): PlaySession =
            if (saved == null || now < saved.lastSeen || now - saved.lastSeen > GAP_MINUTES * MINUTE) PlaySession(now, now)
            else saved.seen(now)

        fun decode(s: String?): PlaySession? {
            val p = s?.split(',') ?: return null
            if (p.size != 3) return null
            val start = p[0].toLongOrNull() ?: return null
            val seen = p[1].toLongOrNull() ?: return null
            return PlaySession(start, seen, p[2] == "1")
        }
    }
}
