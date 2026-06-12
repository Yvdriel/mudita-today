package com.mosquishe.today.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mosquishe.today.TodayApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AlarmManager alarms don't survive a reboot, so re-arm every active future reminder once the
 * device finishes booting.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pending = goAsync()
        val app = context.applicationContext as TodayApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.repository.rescheduleAllReminders()
            } finally {
                pending.finish()
            }
        }
    }
}
