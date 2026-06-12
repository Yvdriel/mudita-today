package com.mosquishe.today.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosquishe.today.data.local.TagEntity
import com.mosquishe.today.data.repo.TaskRepository
import com.mosquishe.today.data.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: TaskRepository,
    private val settings: SettingsStore,
    private val appScope: CoroutineScope,
) : ViewModel() {

    val autoComplete: StateFlow<Boolean> =
        settings.autoComplete.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsStore.DEFAULT_AUTO_COMPLETE)

    /** Raw flow (not stateIn) so the screen can read the real stored value once to seed the picker. */
    val dayStartMinute: Flow<Int> = settings.dayStartMinuteOfDay

    val tags: StateFlow<List<TagEntity>> =
        repo.observeTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val logbookRetentionDays: StateFlow<Int> =
        settings.logbookRetentionDays.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsStore.DEFAULT_LOGBOOK_RETENTION_DAYS,
        )

    /** Chosen reminder sound (null = default tone). Applied to the channel via [AppContainer]. */
    val reminderSound: StateFlow<String?> =
        settings.reminderSound.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setAutoComplete(enabled: Boolean) = viewModelScope.launch { settings.setAutoComplete(enabled) }
    fun setDayStart(minuteOfDay: Int) = viewModelScope.launch { settings.setDayStartMinuteOfDay(minuteOfDay) }
    fun setLogbookRetentionDays(days: Int) = viewModelScope.launch { settings.setLogbookRetentionDays(days) }

    // Tag writes use the app scope, not viewModelScope: leaving Settings right after typing a
    // tag must not cancel the write half-done (one of the things Tobias hit on the Kompakt).
    fun createTag(name: String) = appScope.launch { repo.createTag(name) }
    fun renameTag(tag: TagEntity, name: String) = appScope.launch { repo.renameTag(tag, name) }
    fun deleteTag(tag: TagEntity) = appScope.launch { repo.deleteTag(tag) }
}
