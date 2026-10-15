package com.zeronolidge.speechapp.ui.platform

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberMicPermission(): MicPermission {
    val context = LocalContext.current
    val pending = remember { arrayOfNulls<(Boolean) -> Unit>(1) }
    // Bumped after each answer so the status is read again.
    var answered by remember { mutableStateOf(0) }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        denied = !ok
        answered++
        pending[0]?.invoke(ok)
        pending[0] = null
    }
    return remember(launcher, answered) {
        object : MicPermission {
            override val status: MicStatus
                get() = when {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> MicStatus.ON
                    denied -> MicStatus.BLOCKED
                    else -> MicStatus.NOT_ASKED
                }

            override fun request(onResult: (Boolean) -> Unit) {
                pending[0] = onResult
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)

@Composable
actual fun rememberCopyText(): (String) -> Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        { text: String ->
            runCatching {
                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Talk & Learn backup", text))
            }.isSuccess
        }
    }
}
