package com.mosquishe.today.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/** A to-do with its checklist and tags eagerly loaded. The unit the UI renders. */
data class TaskWithDetails(
    @Embedded val task: TaskEntity,
    @Relation(parentColumn = "id", entityColumn = "taskId")
    val checklist: List<ChecklistItemEntity> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TaskTagCrossRef::class,
            parentColumn = "taskId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity> = emptyList(),
) {
    /** Checklist progress as "done/total"; null when there is no checklist. */
    val checklistProgress: Pair<Int, Int>?
        get() = if (checklist.isEmpty()) null else checklist.count { it.done } to checklist.size
}
