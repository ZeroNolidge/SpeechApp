package com.zeronolidge.speechapp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.zeronolidge.speechapp.audio.AndroidMicLevels
import com.zeronolidge.speechapp.audio.VoiceDetector
import com.zeronolidge.speechapp.audio.AndroidParrot
import com.zeronolidge.speechapp.audio.AndroidVoiceEngine
import com.zeronolidge.speechapp.audio.QueueSpeaker
import com.zeronolidge.speechapp.state.AndroidPrefs
import com.zeronolidge.speechapp.state.Progress
import com.zeronolidge.speechapp.ui.TalkAndLearnApp
import com.zeronolidge.speechapp.ui.components.AppServices

class MainActivity : ComponentActivity() {
    private lateinit var services: AppServices

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val progress = Progress(AndroidPrefs(this))
        services = AppServices(
            speaker = QueueSpeaker(AndroidVoiceEngine(this)).also { it.rate = progress.speechRate },
            voice = VoiceDetector(AndroidMicLevels()),
            parrot = AndroidParrot(this),
            progress = progress,
        )
        setContent { TalkAndLearnApp(services) }
    }

    override fun onPause() {
        super.onPause()
        services.speaker.stop()
        services.voice.cancel()
        services.parrot.stop()
    }

    override fun onDestroy() {
        services.speaker.shutdown()
        services.voice.cancel()
        services.parrot.stop()
        super.onDestroy()
    }
}

