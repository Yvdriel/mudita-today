package com.mosquishe.today.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mosquishe.today.data.local.TaskWithDetails
import com.mosquishe.today.domain.TaskView
import com.mosquishe.today.util.Dates
import com.mudita.mmd.components.checkbox.CheckboxMMD
import com.mudita.mmd.components.text.TextMMD
import java.time.LocalDate

/**
 * One to-do row: a checkbox + title with an optional metadata subline (checklist progress, date /
 * deadline, tags). Uniform layout so scrolling repaints lines in place (Law 3).
 */
@Composable
fun TaskRow(
    task: TaskWithDetails,
    view: TaskView,
    today: LocalDate,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val t = task.task
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CheckboxMMD(checked = t.completed, onCheckedChange = onToggle)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.fillMaxWidth()) {
            TextMMD(
                t.title,
                textDecoration = if (t.completed) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
            )
            subline(task, view, today)?.let { TextMMD(it, fontSize = 13.sp, maxLines = 1) }
            if (task.tags.isNotEmpty()) {
                TextMMD(task.tags.joinToString(" ") { "#${it.name}" }, fontSize = 13.sp, maxLines = 1)
            }
        }
    }
}

private fun subline(task: TaskWithDetails, view: TaskView, today: LocalDate): String? {
    val t = task.task
    val parts = mutableListOf<String>()
    task.checklistProgress?.let { (done, total) -> parts += "$done/$total" }
    when (view) {
        TaskView.LOGBOOK -> t.completedAt?.let { parts += "Completed ${Dates.fullLabel(Dates.instantToLocalDate(it))}" }
        else -> deadlineLabel(t.deadline, today)?.let { parts += it }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("   ·   ")
}

private fun deadlineLabel(deadline: LocalDate?, today: LocalDate): String? = when {
    deadline == null -> null
    deadline.isBefore(today) -> "Overdue"
    deadline == today -> "Due today"
    else -> "Due ${Dates.relativeLabel(deadline, today)}"
}
