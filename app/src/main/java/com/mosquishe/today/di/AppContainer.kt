package com.mosquishe.today.di

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.mosquishe.today.TodayApp
import com.mosquishe.today.data.local.TodayDatabase
import com.mosquishe.today.data.local.TaskWithDetails
import com.mosquishe.today.data.repo.TaskRepository
import com.mosquishe.today.data.settings.SettingsStore
import com.mosquishe.today.reminder.AlarmReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/** Manual dependency container (no Hilt). Built once in [TodayApp.onCreate]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Outlives any screen — used for fire-and-forget cleanup (e.g. discarding empty drafts). */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: TodayDatabase = Room.databaseBuilder(
        appContext,
        TodayDatabase::class.java,
        "today.db",
    ).addMigrations(TodayDatabase.MIGRATION_1_2).build()

    val settings: SettingsStore = SettingsStore(appContext)

    val repository: TaskRepository = TaskRepository(
        taskDao = database.taskDao(),
        tagDao = database.tagDao(),
        settings = settings,
        reminderScheduler = AlarmReminderScheduler(appContext),
    )

    /** Emits a snapshot of a just-deleted to-do so the shell can offer an Undo snackbar. */
    val deletedTaskEvents = MutableSharedFlow<TaskWithDetails>(extraBufferCapacity = 1)

    init {
        applicationScope.launch {
            // Sweep blank drafts left over from a previous force-quit.
            repository.deleteEmptyTasks()
            // Trim the logbook to the retention window, if the user set one.
            repository.pruneLogbook()
            // Re-arm reminders: alarms are cleared on reboot, app update, and force-stop.
            repository.rescheduleAllReminders()
        }
    }
}

/** Convenience accessor for the app-wide [AppContainer] from any composable. */
@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as TodayApp).container

/** Tiny [androidx.lifecycle.ViewModelProvider.Factory] from a constructor lambda (manual DI). */
inline fun <reified VM : androidx.lifecycle.ViewModel> viewModelCreator(
    crossinline make: () -> VM,
): androidx.lifecycle.ViewModelProvider.Factory =
    object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = make() as T
    }
