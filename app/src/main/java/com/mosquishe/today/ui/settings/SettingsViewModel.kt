package com.mosquishe.today.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosquishe.today.data.local.TagEntity
import com.mosquishe.today.data.repo.TaskRepository
import com.mosquishe.today.data.settings.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: TaskRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    val autoComplete: StateFlow<Boolean> =
        settings.autoComplete.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsStore.DEFAULT_AUTO_COMPLETE)

    /** Raw flow (not stateIn) so the screen can read the real stored value once to seed the picker. */
    val dayStartMinute: Flow<Int> = settings.dayStartMinuteOfDay

    val tags: StateFlow<List<TagEntity>> =
        repo.observeTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setAutoComplete(enabled: Boolean) = viewModelScope.launch { settings.setAutoComplete(enabled) }
    fun setDayStart(minuteOfDay: Int) = viewModelScope.launch { settings.setDayStartMinuteOfDay(minuteOfDay) }

    fun createTag(name: String) = viewModelScope.launch { repo.createTag(name) }
    fun renameTag(tag: TagEntity, name: String) = viewModelScope.launch { repo.renameTag(tag, name) }
    fun deleteTag(tag: TagEntity) = viewModelScope.launch { repo.deleteTag(tag) }
}
