package com.mosquishe.today

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mosquishe.today.reminder.EXTRA_TASK_ID
import com.mosquishe.today.ui.nav.AppShell
import com.mosquishe.today.ui.theme.TodayTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /** Set when launched from a reminder notification; the shell consumes it to open the to-do. */
    private val deepLinkTaskId = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeDeepLink(intent)
        setContent {
            TodayTheme {
                AppShell(deepLinkTaskId)
            }
        }
    }

    // launchMode=singleTop: a notification tap while the app is open arrives here, not onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeDeepLink(intent)
    }

    private fun consumeDeepLink(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_TASK_ID, -1L) ?: -1L
        if (id > 0L) deepLinkTaskId.value = id
    }
}
