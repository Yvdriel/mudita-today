package com.mosquishe.today.ui.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosquishe.today.data.local.ChecklistItemEntity
import com.mosquishe.today.data.local.TagEntity
import com.mosquishe.today.data.local.TaskWithDetails
import com.mosquishe.today.data.repo.TaskRepository
import com.mosquishe.today.domain.Recurrence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Backs the add/edit detail screen. A new to-do (requestedId 0) is created immediately so its
 * checklist and tags have a real id to attach to; if the user leaves it blank it's discarded on exit.
 * Title/notes are held as Compose state (the source of truth while typing) and persisted on change;
 * every other field is rendered from the observed [task] flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModel(
    private val repo: TaskRepository,
    private val appScope: CoroutineScope,
    private val deletedTaskEvents: MutableSharedFlow<TaskWithDetails>,
    requestedId: Long,
    defaultEpochDay: Long,
) : ViewModel() {

    val isNew: Boolean = requestedId == 0L
    private val taskId = MutableStateFlow(if (isNew) 0L else requestedId)

    var title by mutableStateOf("")
        private set
    var notes by mutableStateOf("")
        private set

    val task: StateFlow<TaskWithDetails?> =
        taskId.flatMapLatest { id -> if (id == 0L) flowOf(null) else repo.observeTask(id) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val tags: StateFlow<List<TagEntity>> =
        repo.observeTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val today: StateFlow<LocalDate> =
        repo.observeToday().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now())

    init {
        viewModelScope.launch {
            val id = if (isNew) {
                val d = if (defaultEpochDay >= 0) LocalDate.ofEpochDay(defaultEpochDay) else null
                repo.createTask(title = "", scheduledDate = d)
            } else {
                requestedId
            }
            repo.getTask(id)?.let { title = it.task.title; notes = it.task.notes }
            taskId.value = id
        }
    }

    private fun id(): Long = taskId.value

    fun onTitleChange(value: String) { title = value; persist { repo.setTitle(id(), value) } }
    fun onNotesChange(value: String) { notes = value; persist { repo.setNotes(id(), value) } }
    fun setScheduledDate(date: LocalDate?) = persist { repo.setScheduledDate(id(), date) }
    fun setReminder(time: LocalTime?) = persist { repo.setReminder(id(), time) }
    fun setDeadline(date: LocalDate?) = persist { repo.setDeadline(id(), date) }
    fun setRecurrence(rule: Recurrence?) = persist { repo.setRecurrence(id(), rule) }
    fun setCompleted(completed: Boolean) = persist { repo.setTaskCompleted(id(), completed) }

    fun addChecklistItem(text: String) = persist { repo.addChecklistItem(id(), text) }
    fun setItemDone(item: ChecklistItemEntity, done: Boolean) = persist { repo.setChecklistItemDone(item, done) }
    fun setItemText(item: ChecklistItemEntity, text: String) = persist { repo.setChecklistItemText(item, text) }
    fun deleteItem(item: ChecklistItemEntity) = persist { repo.deleteChecklistItem(item) }

    fun toggleTag(tagId: Long, currentlyAssigned: Boolean) = persist {
        val current = (task.value?.tags?.map { it.id } ?: emptyList()).toMutableList()
        if (currentlyAssigned) current.remove(tagId) else current.add(tagId)
        repo.setTaskTags(id(), current.distinct())
    }

    fun createTagAndAssign(name: String) = persist {
        val newId = repo.createTag(name)
        if (newId > 0) {
            val current = (task.value?.tags?.map { it.id } ?: emptyList()) + newId
            repo.setTaskTags(id(), current.distinct())
        }
    }

    fun deleteTask() {
        appScope.launch {
            val snapshot = repo.captureAndDelete(id())
            if (snapshot != null) deletedTaskEvents.tryEmit(snapshot)
        }
    }

    /** Called when leaving the screen: drop a never-filled-in new to-do. */
    fun discardIfEmptyOnExit() {
        if (isNew) appScope.launch { repo.deleteIfEmpty(id()) }
    }

    private inline fun persist(crossinline block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
