package com.zeronolidge.speechapp.state

actual fun today(): Int = java.time.LocalDate.now().toEpochDay().toInt()
