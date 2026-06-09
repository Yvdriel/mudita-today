package com.mosquishe.today.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mosquishe.today.di.appContainer
import com.mosquishe.today.di.viewModelCreator
import com.mosquishe.today.domain.Recurrence
import com.mosquishe.today.ui.common.CalendarSheet
import com.mosquishe.today.util.Dates
import com.mudita.mmd.components.checkbox.CheckboxMMD
import com.mudita.mmd.components.chips.AssistChipMMD
import com.mudita.mmd.components.chips.FilterChipMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

private enum class DateField { WHEN, DEADLINE }

@Composable
fun TaskDetailScreen(taskId: Long, defaultEpochDay: Long, onBack: () -> Unit) {
    val container = appContainer()
    val vm: TaskDetailViewModel = viewModel(
        factory = viewModelCreator {
            TaskDetailViewModel(
                container.repository,
                container.applicationScope,
                container.deletedTaskEvents,
                taskId,
                defaultEpochDay,
            )
        },
    )

    val task by vm.task.collectAsState()
    val allTags by vm.tags.collectAsState()
    val today by vm.today.collectAsState()

    var dateField by remember { mutableStateOf<DateField?>(null) }
    var newItem by remember { mutableStateOf("") }
    var newTag by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }

    fun leave() { vm.discardIfEmptyOnExit(); onBack() }
    BackHandler { leave() }

    val t = task?.task
    val scheduled = t?.scheduledDate
    val deadline = t?.deadline
    val recurrence = t?.recurrence
    val assignedTagIds = task?.tags?.map { it.id }?.toSet() ?: emptySet()
    val checklist = task?.checklist ?: emptyList()

    Column(Modifier.fillMaxSize()) {
        TopAppBarMMD(
            title = { TextMMD(if (vm.isNew) "New to-do" else "Edit") },
            navigationIcon = {
                IconButton(onClick = { leave() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                CheckboxMMD(checked = t?.completed == true, onCheckedChange = { vm.setCompleted(it) })
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }
                DropdownMenuMMD(menuOpen, { menuOpen = false }) {
                    DropdownMenuItemMMD(
                        { TextMMD("Delete") },
                        { menuOpen = false; vm.deleteTask(); onBack() },
                    )
                }
            },
        )

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                TextFieldMMD(
                    value = vm.title,
                    onValueChange = vm::onTitleChange,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { TextMMD("Title") },
                    singleLine = true,
                )
            }
            item {
                TextFieldMMD(
                    value = vm.notes,
                    onValueChange = vm::onNotesChange,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { TextMMD("Notes") },
                )
            }

            // Checklist ---------------------------------------------------
            // Kept right under Notes so checking items off doesn't mean scrolling past
            // every date/tag field (Tobias's suggestion).
            item { HorizontalDividerMMD() }
            item { SectionLabel("Checklist") }
            items(checklist, key = { it.id }) { itemEntity ->
                var text by remember(itemEntity.id) { mutableStateOf(itemEntity.text) }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CheckboxMMD(checked = itemEntity.done, onCheckedChange = { vm.setItemDone(itemEntity, it) })
                    TextFieldMMD(
                        value = text,
                        onValueChange = { text = it; vm.setItemText(itemEntity, it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    IconButton(onClick = { vm.deleteItem(itemEntity) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove")
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextFieldMMD(
                        value = newItem,
                        onValueChange = { newItem = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { TextMMD("Add item") },
                        singleLine = true,
                    )
                    IconButton(onClick = {
                        if (newItem.isNotBlank()) { vm.addChecklistItem(newItem); newItem = "" }
                    }) { Icon(Icons.Filled.Add, contentDescription = "Add item") }
                }
            }

            item { HorizontalDividerMMD() }

            // When --------------------------------------------------------
            item { SectionLabel("When") }
            item {
                ChipRow {
                    FilterChipMMD(scheduled == today, { vm.setScheduledDate(today) }, { TextMMD("Today") })
                    FilterChipMMD(scheduled == today.plusDays(1), { vm.setScheduledDate(today.plusDays(1)) }, { TextMMD("Tomorrow") })
                    FilterChipMMD(scheduled == null, { vm.setScheduledDate(null) }, { TextMMD("Someday") })
                    val custom = scheduled != null && scheduled != today && scheduled != today.plusDays(1)
                    AssistChipMMD(
                        { dateField = DateField.WHEN },
                        { TextMMD(if (custom) Dates.relativeLabel(scheduled!!, today) else "Pick date") },
                    )
                }
            }

            // Deadline ----------------------------------------------------
            item { SectionLabel("Deadline") }
            item {
                ChipRow {
                    AssistChipMMD(
                        { dateField = DateField.DEADLINE },
                        { TextMMD(deadline?.let { "Due ${Dates.relativeLabel(it, today)}" } ?: "Add deadline") },
                    )
                    if (deadline != null) {
                        AssistChipMMD({ vm.setDeadline(null) }, { TextMMD("Clear") })
                    }
                }
            }

            // Repeat ------------------------------------------------------
            item { SectionLabel("Repeat") }
            item {
                ChipRow {
                    FilterChipMMD(recurrence == null, { vm.setRecurrence(null) }, { TextMMD("None") })
                    FilterChipMMD(recurrence == Recurrence.DAILY, { vm.setRecurrence(Recurrence.DAILY) }, { TextMMD("Daily") })
                    FilterChipMMD(recurrence == Recurrence.WEEKLY, { vm.setRecurrence(Recurrence.WEEKLY) }, { TextMMD("Weekly") })
                    FilterChipMMD(recurrence == Recurrence.MONTHLY, { vm.setRecurrence(Recurrence.MONTHLY) }, { TextMMD("Monthly") })
                }
            }

            // Tags --------------------------------------------------------
            item { SectionLabel("Tags") }
            if (allTags.isNotEmpty()) {
                item {
                    ChipRow {
                        allTags.forEach { tag ->
                            val assigned = tag.id in assignedTagIds
                            FilterChipMMD(assigned, { vm.toggleTag(tag.id, assigned) }, { TextMMD(tag.name) })
                        }
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextFieldMMD(
                        value = newTag,
                        onValueChange = { newTag = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { TextMMD("New tag") },
                        singleLine = true,
                    )
                    IconButton(onClick = {
                        if (newTag.isNotBlank()) { vm.createTagAndAssign(newTag); newTag = "" }
                    }) { Icon(Icons.Filled.Add, contentDescription = "Add tag") }
                }
            }
        }
    }

    when (dateField) {
        DateField.WHEN -> CalendarSheet(
            initial = scheduled ?: today,
            onResult = { vm.setScheduledDate(it) },
            onDismiss = { dateField = null },
        )
        DateField.DEADLINE -> CalendarSheet(
            initial = deadline ?: today,
            onResult = { vm.setDeadline(it) },
            onDismiss = { dateField = null },
        )
        null -> Unit
    }
}

@Composable
private fun SectionLabel(text: String) {
    TextMMD(text, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}
