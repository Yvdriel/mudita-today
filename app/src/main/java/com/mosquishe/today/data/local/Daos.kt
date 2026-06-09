package com.mosquishe.today.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface TaskDao {

    @Transaction
    @Query("SELECT * FROM tasks")
    fun observeAll(): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTask(id: Long): Flow<TaskWithDetails?>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTask(id: Long): TaskWithDetails?

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskEntity(id: Long): TaskEntity?

    @Insert suspend fun insertTask(task: TaskEntity): Long
    @Update suspend fun updateTask(task: TaskEntity)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteTaskById(id: Long)
    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM tasks") suspend fun maxSortOrder(): Long

    /** Empty the logbook (every completed to-do). */
    @Query("DELETE FROM tasks WHERE completed = 1") suspend fun deleteAllCompleted()

    /** Drop completed to-dos finished before [threshold] (logbook auto-prune). */
    @Query("DELETE FROM tasks WHERE completed = 1 AND completedAt IS NOT NULL AND completedAt < :threshold")
    suspend fun deleteCompletedBefore(threshold: Instant)

    /** Sweep blank drafts orphaned by a force-quit (no title, notes, or checklist). */
    @Query(
        "DELETE FROM tasks WHERE TRIM(title) = '' AND TRIM(notes) = '' " +
            "AND id NOT IN (SELECT taskId FROM checklist_items)",
    )
    suspend fun deleteEmptyTasks()

    // Checklist
    @Query("SELECT * FROM checklist_items WHERE taskId = :taskId ORDER BY sortOrder")
    suspend fun checklistFor(taskId: Long): List<ChecklistItemEntity>

    @Insert suspend fun insertChecklistItem(item: ChecklistItemEntity): Long
    @Update suspend fun updateChecklistItem(item: ChecklistItemEntity)
    @Delete suspend fun deleteChecklistItem(item: ChecklistItemEntity)
    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM checklist_items WHERE taskId = :taskId")
    suspend fun maxChecklistOrder(taskId: Long): Int

    // Tag links
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addTagToTask(ref: TaskTagCrossRef)
    @Delete suspend fun removeTagFromTask(ref: TaskTagCrossRef)
    @Query("DELETE FROM task_tag WHERE taskId = :taskId") suspend fun clearTaskTags(taskId: Long)
    @Query("SELECT tagId FROM task_tag WHERE taskId = :taskId") suspend fun tagIdsFor(taskId: Long): List<Long>
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id") suspend fun getById(id: Long): TagEntity?
    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): TagEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(tag: TagEntity): Long
    @Update suspend fun update(tag: TagEntity)
    @Delete suspend fun delete(tag: TagEntity)
    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM tags") suspend fun maxSortOrder(): Int
}
