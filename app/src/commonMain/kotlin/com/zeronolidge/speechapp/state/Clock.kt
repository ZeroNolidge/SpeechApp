package com.zeronolidge.speechapp.state

/** Today as a day number in the device's own time zone, so "a new day" starts at local midnight. */
expect fun today(): Int

/** The wall-clock time in milliseconds, for a play session that survives a reload. */
expect fun nowMillis(): Long
