package com.mosquishe.today.ui.detail

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mosquishe.today.di.appContainer
import com.mosquishe.today.di.viewModelCreator
import com.mosquishe.today.domain.Recurrence
import com.mosquishe.today.ui.common.CalendarSheet
import com.mosquishe.today.ui.common.TimeSheet
import com.mosquishe.today.util.Dates
import java.time.LocalTime
import com.mudita.mmd.components.checkbox.CheckboxMMD
import com.mudita.mmd.components.chips.AssistChipMMD
import com.mudita.mmd.components.chips.FilterChipMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
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
    var showReminder by remember { mutableStateOf(false) }
    var newItem by remember { mutableStateOf("") }
    var newTag by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }

    // Reminders post notifications; on Android 13+ that needs a runtime grant. Ask when the user
    // adds one — scheduling still proceeds, the notification just won't show until granted.
    val context = LocalContext.current
    val notifPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun leave() { vm.discardIfEmptyOnExit(); onBack() }
    BackHandler { leave() }

    val t = task?.task
    val scheduled = t?.scheduledDate
    val deadline = t?.deadline
    val reminder = t?.reminderTime
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

        // Each item below is one substantial section, never a bare divider or label. MMD's
        // scrollbar assumes every item is the height of the first visible one
        // (canScroll = totalItems * firstVisibleItem.size > viewport); tiny divider/label items
        // make that estimate collapse at the bottom and the scrollbar vanishes. weight(1f) (not
        // fillMaxSize) gives the list its real remaining height. See the MangaShelf MMD docs.
        LazyColumnMMD(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 32.dp)) {
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

            // Checklist — one section item (kept under Notes so ticking items off doesn't mean
            // scrolling past the date/tag fields).
            item {
                Column {
                    HorizontalDividerMMD()
                    SectionLabel("Checklist")
                    checklist.forEach { itemEntity ->
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
            }

            // When
            item {
                Column {
                    HorizontalDividerMMD()
                    SectionLabel("When")
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
            }

            // Reminder — a time-of-day on the When date (Things3-style).
            item {
                Column {
                    SectionLabel("Reminder")
                    ChipRow {
                        AssistChipMMD(
                            { ensureNotificationPermission(); showReminder = true },
                            { TextMMD(reminder?.let { Dates.timeLabel(it) } ?: "Add reminder") },
                        )
                        if (reminder != null) {
                            AssistChipMMD({ vm.setReminder(null) }, { TextMMD("Clear") })
                        }
                    }
                }
            }

            // Deadline
            item {
                Column {
                    SectionLabel("Deadline")
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
            }

            // Repeat
            item {
                Column {
                    SectionLabel("Repeat")
                    ChipRow {
                        FilterChipMMD(recurrence == null, { vm.setRecurrence(null) }, { TextMMD("None") })
                        FilterChipMMD(recurrence == Recurrence.DAILY, { vm.setRecurrence(Recurrence.DAILY) }, { TextMMD("Daily") })
                        FilterChipMMD(recurrence == Recurrence.WEEKLY, { vm.setRecurrence(Recurrence.WEEKLY) }, { TextMMD("Weekly") })
                        FilterChipMMD(recurrence == Recurrence.MONTHLY, { vm.setRecurrence(Recurrence.MONTHLY) }, { TextMMD("Monthly") })
                    }
                }
            }

            // Tags
            item {
                Column {
                    SectionLabel("Tags")
                    if (allTags.isNotEmpty()) {
                        ChipRow {
                            allTags.forEach { tag ->
                                val assigned = tag.id in assignedTagIds
                                FilterChipMMD(assigned, { vm.toggleTag(tag.id, assigned) }, { TextMMD(tag.name) })
                            }
                        }
                    }
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

    if (showReminder) {
        TimeSheet(
            initial = reminder ?: LocalTime.of(9, 0),
            onResult = { vm.setReminder(it) },
            onDismiss = { showReminder = false },
        )
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
