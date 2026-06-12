package com.mosquishe.today.ui.settings

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mosquishe.today.data.settings.SettingsStore
import com.mudita.mmd.components.bottom_sheet.ModalBottomSheetMMD
import com.mudita.mmd.components.bottom_sheet.rememberModalBottomSheetMMDState
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD

/** One selectable sound. [value] follows the store convention: null = default, "silent", or a URI. */
data class ToneOption(val title: String, val value: String?)

/** System notification tones available on the device (gentle / notification stream). */
fun systemNotificationTones(context: Context): List<ToneOption> {
    val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_NOTIFICATION) }
    val tones = mutableListOf<ToneOption>()
    val cursor = manager.cursor
    while (cursor.moveToNext()) {
        val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
        val uri = manager.getRingtoneUri(cursor.position)?.toString() ?: continue
        tones += ToneOption(title, uri)
    }
    return tones
}

/** Human label for a stored reminder-sound value (for the Settings row). */
fun reminderSoundLabel(context: Context, value: String?): String = when (value) {
    null -> "Default"
    SettingsStore.REMINDER_SOUND_SILENT -> "Silent"
    else -> runCatching {
        RingtoneManager.getRingtone(context, Uri.parse(value))?.getTitle(context)
    }.getOrNull() ?: "Custom"
}

private fun toneUri(value: String?): Uri? = when (value) {
    null -> Settings.System.DEFAULT_NOTIFICATION_URI
    SettingsStore.REMINDER_SOUND_SILENT -> null
    else -> Uri.parse(value)
}

/**
 * Bottom sheet to pick the reminder notification sound from the device's notification tones.
 * Tapping a row previews it (on the gentle notification stream); Done returns the selection.
 */
@Composable
fun ReminderSoundSheet(
    current: String?,
    onResult: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val options = remember {
        buildList {
            add(ToneOption("Default", null))
            add(ToneOption("Silent", SettingsStore.REMINDER_SOUND_SILENT))
            addAll(systemNotificationTones(context))
        }
    }
    var selected by remember { mutableStateOf(current) }
    var preview by remember { mutableStateOf<Ringtone?>(null) }
    DisposableEffect(Unit) { onDispose { preview?.stop() } }

    fun play(value: String?) {
        preview?.stop()
        val uri = toneUri(value)
        preview = if (uri == null) {
            null
        } else {
            RingtoneManager.getRingtone(context, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                play()
            }
        }
    }

    val sheetState = rememberModalBottomSheetMMDState(true) // skipPartiallyExpanded: open fully, no half-drag
    ModalBottomSheetMMD(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            LazyColumnMMD(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                items(options, key = { it.value ?: "default" }) { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selected = option.value; play(option.value) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextMMD(option.title, modifier = Modifier.weight(1f))
                        if (selected == option.value) {
                            Icon(Icons.Filled.Check, contentDescription = "Selected")
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButtonMMD(onClick = onDismiss) { TextMMD("Cancel") }
                Spacer(Modifier.width(8.dp))
                ButtonMMD(
                    onClick = {
                        preview?.stop()
                        onResult(selected)
                        onDismiss()
                    },
                ) { TextMMD("Done") }
            }
        }
    }
}
