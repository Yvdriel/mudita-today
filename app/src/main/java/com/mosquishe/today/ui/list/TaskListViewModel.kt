package com.mosquishe.today.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosquishe.today.data.local.ChecklistItemEntity
import com.mosquishe.today.data.local.TagEntity
import com.mosquishe.today.data.local.TaskWithDetails
import com.mosquishe.today.data.repo.TaskRepository
import com.mosquishe.today.domain.TaskView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModel(
    private val repo: TaskRepository,
    val view: TaskView,
) : ViewModel() {

    private val tagFilter = MutableStateFlow<Long?>(null)
    val selectedTagId: StateFlow<Long?> = tagFilter.asStateFlow()

    private val query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = query.asStateFlow()

    val tags: StateFlow<List<TagEntity>> =
        repo.observeTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val today: StateFlow<LocalDate> =
        repo.observeToday().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now())

    val tasks: StateFlow<List<TaskWithDetails>> =
        combine(
            tagFilter.flatMapLatest { repo.observeView(view, it) },
            query,
        ) { list, q ->
            if (q.isBlank()) list
            else list.filter { it.task.title.contains(q.trim(), ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectTag(id: Long?) { tagFilter.value = id }
    fun setQuery(q: String) { query.value = q }

    fun setCompleted(taskId: Long, completed: Boolean) {
        viewModelScope.launch { repo.setTaskCompleted(taskId, completed) }
    }

    fun setItemDone(item: ChecklistItemEntity, done: Boolean) {
        viewModelScope.launch { repo.setChecklistItemDone(item, done) }
    }

    fun clearLogbook() {
        viewModelScope.launch { repo.clearLogbook() }
    }
}
