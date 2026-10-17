package com.zeronolidge.speechapp.state

import kotlin.js.js

private fun jsToday(): Int = js("(() => { const d = new Date(); return Math.floor((d.getTime() - d.getTimezoneOffset() * 60000) / 86400000); })()")

actual fun today(): Int = jsToday()

private fun jsNow(): Double = js("Date.now()")

actual fun nowMillis(): Long = jsNow().toLong()
