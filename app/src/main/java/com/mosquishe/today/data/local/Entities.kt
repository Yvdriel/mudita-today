package com.mosquishe.today.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mosquishe.today.domain.Recurrence
import java.time.Instant
import java.time.LocalDate

/** A to-do. Dates use [LocalDate] (null scheduledDate = Anytime). Times stored via [Converters]. */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val scheduledDate: LocalDate? = null,
    val deadline: LocalDate? = null,
    val recurrence: Recurrence? = null,
    val completed: Boolean = false,
    val completedAt: Instant? = null,
    val createdAt: Instant = Instant.EPOCH,
    val sortOrder: Long = 0,
)

/** One line of a to-do's checklist. */
@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId")],
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val text: String,
    val done: Boolean = false,
    val sortOrder: Int = 0,
)

/** A user-created tag. Name is unique. */
@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
)

/** Many-to-many join: a to-do can carry several tags. */
@Entity(
    tableName = "task_tag",
    primaryKeys = ["taskId", "tagId"],
    foreignKeys = [
        ForeignKey(TaskEntity::class, ["id"], ["taskId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(TagEntity::class, ["id"], ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("tagId")],
)
data class TaskTagCrossRef(
    val taskId: Long,
    val tagId: Long,
)
