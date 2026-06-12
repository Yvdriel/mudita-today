package com.mosquishe.today.reminder

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.mosquishe.today.MainActivity
import com.mosquishe.today.ui.theme.TodayTheme
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD

/**
 * Full-screen reminder alert shown OVER the lock screen. The Kompakt's launcher gates third-party
 * notifications off the lock screen, but a full-screen intent launches an activity (not a shade
 * entry), which the allow-list can't filter. Calm by design: it lights the screen and shows the
 * to-do, with no alarm sound or looping (the single channel ping is the only sound).
 */
class ReminderAlertActivity : ComponentActivity() {

    private val alert = mutableStateOf(AlertData())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show over the keyguard and wake the screen (also set as manifest attributes for reliability).
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        alert.value = readIntent(intent)

        setContent {
            val data by alert
            TodayTheme {
                ReminderAlert(
                    title = data.title,
                    time = data.time,
                    onOpen = { openTask(data.taskId) },
                    onDismiss = { dismiss(data.taskId) },
                )
            }
        }
    }

    // A second reminder firing while this is showing replaces the displayed one (launchMode=singleInstance).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        alert.value = readIntent(intent)
    }

    private fun readIntent(intent: Intent?): AlertData = AlertData(
        taskId = intent?.getLongExtra(EXTRA_TASK_ID, -1L) ?: -1L,
        title = intent?.getStringExtra(EXTRA_TASK_TITLE)?.ifBlank { "To-do" } ?: "To-do",
        time = intent?.getStringExtra(EXTRA_TASK_TIME).orEmpty(),
    )

    /** Open the to-do (dismissing a secure keyguard first), then close the alert. */
    private fun openTask(taskId: Long) {
        NotificationManagerCompat.from(this).cancel(taskId.toInt())
        val open = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val keyguard = getSystemService<KeyguardManager>()
        if (keyguard?.isKeyguardLocked == true) {
            keyguard.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        startActivity(open)
                        finish()
                    }
                },
            )
        } else {
            startActivity(open)
            finish()
        }
    }

    private fun dismiss(taskId: Long) {
        NotificationManagerCompat.from(this).cancel(taskId.toInt())
        finish()
    }
}

private data class AlertData(val taskId: Long = -1L, val title: String = "", val time: String = "")

@Composable
private fun ReminderAlert(
    title: String,
    time: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextMMD("Reminder", fontSize = 14.sp)
            TextMMD(title, fontSize = 28.sp)
            if (time.isNotBlank()) TextMMD(time, fontSize = 18.sp)
            ButtonMMD(onClick = onOpen) { TextMMD("Open") }
            OutlinedButtonMMD(onClick = onDismiss) { TextMMD("Dismiss") }
        }
    }
}
