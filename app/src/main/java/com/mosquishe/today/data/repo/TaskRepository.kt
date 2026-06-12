package com.mosquishe.today.data.repo

import com.mosquishe.today.data.local.ChecklistItemEntity
import com.mosquishe.today.data.local.TagDao
import com.mosquishe.today.data.local.TagEntity
import com.mosquishe.today.data.local.TaskDao
import com.mosquishe.today.data.local.TaskEntity
import com.mosquishe.today.data.local.TaskTagCrossRef
import com.mosquishe.today.data.local.TaskWithDetails
import com.mosquishe.today.data.settings.SettingsStore
import com.mosquishe.today.domain.Completion
import com.mosquishe.today.domain.DateLogic
import com.mosquishe.today.domain.Recurrence
import com.mosquishe.today.domain.TaskView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Single gateway between the UI and persistence. Owns the view filtering (delegating the rules to
 * [DateLogic]), checklist auto-completion ([Completion]) and recurrence spawning ([Recurrence]).
 *
 * View filtering happens in memory over [TaskDao.observeAll] — a personal to-do list is small, and
 * this keeps the view rules in one tested place rather than spread across SQL.
 */
class TaskRepository(
    private val taskDao: TaskDao,
    private val tagDao: TagDao,
    private val settings: SettingsStore,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) {

    // ---- Reads -------------------------------------------------------------

    fun observeTags(): Flow<List<TagEntity>> = tagDao.observeTags()

    fun observeTask(id: Long): Flow<TaskWithDetails?> = taskDao.observeTask(id)

    /** The logical "today", recomputed whenever the day-start setting changes. */
    fun observeToday(): Flow<LocalDate> =
        settings.dayStart.map { DateLogic.logicalToday(now(), it) }

    /** Tasks for [view], optionally filtered to [tagId], correctly sorted for that view. */
    fun observeView(view: TaskView, tagId: Long?): Flow<List<TaskWithDetails>> =
        combine(taskDao.observeAll(), settings.dayStart) { all, dayStart ->
            val today = DateLogic.logicalToday(now(), dayStart)
            all.asSequence()
                .filter { matchesView(it, view, today) }
                .filter { tagId == null || it.tags.any { tag -> tag.id == tagId } }
                .sortedWith(comparatorFor(view))
                .toList()
        }

    private fun matchesView(t: TaskWithDetails, view: TaskView, today: LocalDate): Boolean {
        val task = t.task
        return when (view) {
            TaskView.TODAY -> DateLogic.isInToday(task.scheduledDate, task.deadline, task.completed, today)
            TaskView.ANYTIME -> DateLogic.isInAnytime(task.scheduledDate, task.deadline, task.completed, today)
            TaskView.UPCOMING -> DateLogic.isInUpcoming(task.scheduledDate, task.completed, today)
            TaskView.LOGBOOK -> DateLogic.isInLogbook(task.completed)
        }
    }

    private fun comparatorFor(view: TaskView): Comparator<TaskWithDetails> = when (view) {
        TaskView.UPCOMING -> compareBy({ it.task.scheduledDate }, { it.task.sortOrder })
        TaskView.LOGBOOK -> compareByDescending { it.task.completedAt ?: Instant.EPOCH }
        else -> compareBy({ it.task.sortOrder }, { it.task.createdAt })
    }

    // ---- Task mutations ----------------------------------------------------

    suspend fun createTask(
        title: String,
        notes: String = "",
        scheduledDate: LocalDate? = null,
        deadline: LocalDate? = null,
        recurrence: Recurrence? = null,
        tagIds: List<Long> = emptyList(),
    ): Long {
        val id = taskDao.insertTask(
            TaskEntity(
                title = title.trim(),
                notes = notes,
                scheduledDate = scheduledDate,
                deadline = deadline,
                recurrence = recurrence,
                createdAt = Instant.now(),
                sortOrder = taskDao.maxSortOrder() + 1,
            ),
        )
        tagIds.forEach { taskDao.addTagToTask(TaskTagCrossRef(id, it)) }
        return id
    }

    suspend fun getTask(taskId: Long): TaskWithDetails? = taskDao.getTask(taskId)

    // Granular field setters — the detail screen persists each edit immediately (Things3-style).
    suspend fun setTitle(taskId: Long, title: String) = edit(taskId) { it.copy(title = title) }
    suspend fun setNotes(taskId: Long, notes: String) = edit(taskId) { it.copy(notes = notes) }
    suspend fun setScheduledDate(taskId: Long, date: LocalDate?) = edit(taskId) { it.copy(scheduledDate = date) }
    suspend fun setDeadline(taskId: Long, date: LocalDate?) = edit(taskId) { it.copy(deadline = date) }
    suspend fun setRecurrence(taskId: Long, recurrence: Recurrence?) = edit(taskId) { it.copy(recurrence = recurrence) }

    private suspend inline fun edit(taskId: Long, transform: (TaskEntity) -> TaskEntity) {
        val t = taskDao.getTaskEntity(taskId) ?: return
        taskDao.updateTask(transform(t))
    }

    suspend fun deleteTask(taskId: Long) = taskDao.deleteTaskById(taskId)

    /** Delete a to-do but return a full snapshot first, so it can be [restore]d (undo). */
    suspend fun captureAndDelete(taskId: Long): TaskWithDetails? {
        val snapshot = taskDao.getTask(taskId) ?: return null
        taskDao.deleteTaskById(taskId)
        return snapshot
    }

    /** Re-insert a deleted to-do (new id) with its checklist and tag links. Tags themselves persist. */
    suspend fun restore(snapshot: TaskWithDetails) {
        val newId = taskDao.insertTask(snapshot.task.copy(id = 0))
        snapshot.checklist.forEach { taskDao.insertChecklistItem(it.copy(id = 0, taskId = newId)) }
        snapshot.tags.forEach { taskDao.addTagToTask(TaskTagCrossRef(newId, it.id)) }
    }

    /** Sweep blank drafts orphaned by a force-quit. Called once at startup. */
    suspend fun deleteEmptyTasks() = taskDao.deleteEmptyTasks()

    /** Delete a freshly-created to-do that the user left blank (no title, notes or checklist). */
    suspend fun deleteIfEmpty(taskId: Long) {
        val t = taskDao.getTask(taskId) ?: return
        if (t.task.title.isBlank() && t.task.notes.isBlank() && t.checklist.isEmpty()) {
            taskDao.deleteTaskById(taskId)
        }
    }

    /** Manual complete/uncomplete toggle (list checkbox or detail). Spawns the next recurrence. */
    suspend fun setTaskCompleted(taskId: Long, completed: Boolean) {
        val t = taskDao.getTaskEntity(taskId) ?: return
        if (t.completed == completed) return
        applyCompletion(t, completed)
    }

    private suspend fun applyCompletion(task: TaskEntity, completed: Boolean) {
        taskDao.updateTask(
            task.copy(completed = completed, completedAt = if (completed) Instant.now() else null),
        )
        if (completed && task.recurrence != null) spawnNextOccurrence(task)
    }

    private suspend fun spawnNextOccurrence(task: TaskEntity) {
        val rule = task.recurrence ?: return
        val base = task.scheduledDate ?: DateLogic.logicalToday(now(), settings.dayStartValue())
        val newId = taskDao.insertTask(
            task.copy(
                id = 0,
                scheduledDate = rule.nextDate(base),
                completed = false,
                completedAt = null,
                createdAt = Instant.now(),
                sortOrder = taskDao.maxSortOrder() + 1,
            ),
        )
        taskDao.checklistFor(task.id).forEach {
            taskDao.insertChecklistItem(it.copy(id = 0, taskId = newId, done = false))
        }
        taskDao.tagIdsFor(task.id).forEach { taskDao.addTagToTask(TaskTagCrossRef(newId, it)) }
    }

    // ---- Checklist ---------------------------------------------------------

    suspend fun addChecklistItem(taskId: Long, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        taskDao.insertChecklistItem(
            ChecklistItemEntity(
                taskId = taskId,
                text = trimmed,
                sortOrder = taskDao.maxChecklistOrder(taskId) + 1,
            ),
        )
        recomputeCompletion(taskId)
    }

    suspend fun setChecklistItemDone(item: ChecklistItemEntity, done: Boolean) {
        taskDao.updateChecklistItem(item.copy(done = done))
        recomputeCompletion(item.taskId)
    }

    suspend fun setChecklistItemText(item: ChecklistItemEntity, text: String) {
        taskDao.updateChecklistItem(item.copy(text = text.trim()))
    }

    suspend fun deleteChecklistItem(item: ChecklistItemEntity) {
        taskDao.deleteChecklistItem(item)
        recomputeCompletion(item.taskId)
    }

    /** Re-derive completion from the checklist + the auto-complete setting after any checklist edit. */
    private suspend fun recomputeCompletion(taskId: Long) {
        val task = taskDao.getTaskEntity(taskId) ?: return
        val checklistDone = taskDao.checklistFor(taskId).map { it.done }
        val newCompleted = Completion.deriveCompleted(task.completed, checklistDone, settings.autoCompleteValue())
        if (newCompleted != task.completed) applyCompletion(task, newCompleted)
    }

    // ---- Tags --------------------------------------------------------------

    /** Create a tag (or return the existing one with the same name, case-insensitive). */
    suspend fun createTag(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1
        tagDao.getByName(trimmed)?.let { return it.id }
        val id = tagDao.insert(TagEntity(name = trimmed, sortOrder = tagDao.maxSortOrder() + 1))
        return if (id == -1L) tagDao.getByName(trimmed)?.id ?: -1 else id
    }

    suspend fun renameTag(tag: TagEntity, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) tagDao.update(tag.copy(name = trimmed))
    }

    suspend fun deleteTag(tag: TagEntity) = tagDao.delete(tag)

    // ---- Logbook maintenance ----------------------------------------------

    /** Empty the logbook (delete every completed to-do). */
    suspend fun clearLogbook() = taskDao.deleteAllCompleted()

    /** Drop completed to-dos older than the retention window. No-op when retention is off (0). */
    suspend fun pruneLogbook() {
        val days = settings.logbookRetentionDaysValue()
        if (days <= 0) return
        taskDao.deleteCompletedBefore(Instant.now().minus(Duration.ofDays(days.toLong())))
    }

    suspend fun setTaskTags(taskId: Long, tagIds: List<Long>) {
        taskDao.clearTaskTags(taskId)
        tagIds.forEach { taskDao.addTagToTask(TaskTagCrossRef(taskId, it)) }
    }
}
