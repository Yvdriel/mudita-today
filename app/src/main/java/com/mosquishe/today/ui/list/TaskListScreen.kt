package com.mosquishe.today.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import com.mosquishe.today.domain.TaskView
import com.mosquishe.today.ui.common.ConfirmSheet
import com.mosquishe.today.ui.common.EmptyState
import com.mosquishe.today.ui.common.TagFilterBar
import com.mosquishe.today.ui.common.TaskRow
import com.mosquishe.today.util.Dates
import com.mudita.mmd.components.buttons.FloatingActionButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import java.time.LocalDate

/** The four views share this one screen; behavior (grouping, FAB, default date) varies by [view]. */
@Composable
fun TaskListScreen(
    view: TaskView,
    onOpenTask: (taskId: Long, defaultEpochDay: Long?) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val container = appContainer()
    val vm: TaskListViewModel = viewModel(
        key = view.name,
        factory = viewModelCreator { TaskListViewModel(container.repository, view) },
    )
    val tasks by vm.tasks.collectAsState()
    val tags by vm.tags.collectAsState()
    val selectedTag by vm.selectedTagId.collectAsState()
    val today by vm.today.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()

    var menuOpen by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBarMMD(
                title = { TextMMD(title(view)) },
                actions = {
                    if (view == TaskView.LOGBOOK) {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenuMMD(menuOpen, { menuOpen = false }) {
                            DropdownMenuItemMMD(
                                { TextMMD("Clear logbook") },
                                { menuOpen = false; showClearConfirm = true },
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
            if (view == TaskView.LOGBOOK) {
                TextFieldMMD(
                    value = searchQuery,
                    onValueChange = vm::setQuery,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { TextMMD("Search logbook") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { vm.setQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                )
                HorizontalDividerMMD()
            }

            if (tags.isNotEmpty()) {
                TagFilterBar(tags, selectedTag, vm::selectTag)
                HorizontalDividerMMD()
            }

            if (tasks.isEmpty()) {
                EmptyState(
                    if (view == TaskView.LOGBOOK && searchQuery.isNotBlank()) "No matches." else emptyMessage(view),
                )
            } else {
                LazyColumnMMD(Modifier.fillMaxSize()) {
                    if (view == TaskView.UPCOMING) {
                        tasks.groupBy { it.task.scheduledDate }.forEach { (date, group) ->
                            if (date != null) {
                                item(key = "header-$date") {
                                    TextMMD(
                                        Dates.relativeLabel(date, today),
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                                    )
                                    HorizontalDividerMMD()
                                }
                            }
                            items(group, key = { it.task.id }) { row(it, view, today, vm, onOpenTask) }
                        }
                    } else {
                        items(tasks, key = { it.task.id }) { row(it, view, today, vm, onOpenTask) }
                    }
                }
            }
        }

        if (view != TaskView.LOGBOOK) {
            FloatingActionButtonMMD(
                onClick = { onOpenTask(0L, defaultEpochDay(view, today)) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New to-do")
            }
        }

        if (showClearConfirm) {
            ConfirmSheet(
                title = "Clear logbook?",
                message = "This permanently deletes every completed to-do.",
                confirmLabel = "Clear",
                onConfirm = { vm.clearLogbook(); showClearConfirm = false },
                onDismiss = { showClearConfirm = false },
            )
        }
    }
}

@Composable
private fun row(
    task: com.mosquishe.today.data.local.TaskWithDetails,
    view: TaskView,
    today: LocalDate,
    vm: TaskListViewModel,
    onOpenTask: (Long, Long?) -> Unit,
) {
    TaskRow(
        task = task,
        view = view,
        today = today,
        onToggle = { checked -> vm.setCompleted(task.task.id, checked) },
        onClick = { onOpenTask(task.task.id, null) },
        onToggleItem = { item, done -> vm.setItemDone(item, done) },
    )
    HorizontalDividerMMD()
}

private fun title(view: TaskView): String = when (view) {
    TaskView.TODAY -> "Today"
    TaskView.UPCOMING -> "Upcoming"
    TaskView.ANYTIME -> "Anytime"
    TaskView.LOGBOOK -> "Logbook"
}

private fun emptyMessage(view: TaskView): String = when (view) {
    TaskView.TODAY -> "Nothing for today.\nTap + to add a to-do."
    TaskView.UPCOMING -> "No scheduled to-dos yet."
    TaskView.ANYTIME -> "No undated to-dos.\nTap + to add one."
    TaskView.LOGBOOK -> "Completed to-dos will appear here."
}

private fun defaultEpochDay(view: TaskView, today: LocalDate): Long? = when (view) {
    TaskView.TODAY -> today.toEpochDay()
    TaskView.UPCOMING -> today.plusDays(1).toEpochDay()
    TaskView.ANYTIME -> null
    TaskView.LOGBOOK -> null
}
