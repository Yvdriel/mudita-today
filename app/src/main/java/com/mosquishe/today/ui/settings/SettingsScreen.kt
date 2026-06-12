package com.mosquishe.today.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.mudita.mmd.components.chips.FilterChipMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.mudita.mmd.components.time.TimeInputMMD
import com.mudita.mmd.components.time.rememberTimeInputMMDState
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import kotlinx.coroutines.flow.first

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = appContainer()
    val vm: SettingsViewModel = viewModel(
        factory = viewModelCreator { SettingsViewModel(container.repository, container.settings, container.applicationScope) },
    )

    val autoComplete by vm.autoComplete.collectAsState()
    val tags by vm.tags.collectAsState()

    var newTag by remember { mutableStateOf("") }
    var dayStartSeed by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) { dayStartSeed = vm.dayStartMinute.first() }

    Column(Modifier.fillMaxSize()) {
        TopAppBarMMD(
            title = { TextMMD("Settings") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        // Section headers are substantial items (divider + label folded in); the tag rows stay
        // individual and uniform. MMD estimates "scrollable" as totalItems * firstVisibleItem.size
        // > viewport, so no item may be tiny (a bare divider/label) and none may be a single item
        // taller than the viewport (don't lump the whole tag list into one item). weight(1f) gives
        // the list its real remaining height. See the MangaShelf MMD docs.
        LazyColumnMMD(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextMMD("Complete to-do when checklist finished", modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(12.dp))
                        SwitchMMD(checked = autoComplete, onCheckedChange = { vm.setAutoComplete(it) })
                    }
                    HorizontalDividerMMD()
                }
            }

            item {
                Column {
                    SectionLabel("Day starts at")
                    val seed = dayStartSeed
                    if (seed != null) {
                        key(seed) {
                            val state = rememberTimeInputMMDState(seed / 60, seed % 60, true)
                            LaunchedEffect(state.hour, state.minute) {
                                vm.setDayStart(state.hour * 60 + state.minute)
                            }
                            TimeInputMMD(state, Modifier.padding(horizontal = 16.dp))
                        }
                    }
                    HorizontalDividerMMD()
                }
            }

            item {
                Column {
                    SectionLabel("Keep completed to-dos")
                    val retention by vm.logbookRetentionDays.collectAsState()
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChipMMD(retention == 0, { vm.setLogbookRetentionDays(0) }, { TextMMD("Forever") })
                        FilterChipMMD(retention == 30, { vm.setLogbookRetentionDays(30) }, { TextMMD("30 days") })
                        FilterChipMMD(retention == 90, { vm.setLogbookRetentionDays(90) }, { TextMMD("90 days") })
                    }
                    HorizontalDividerMMD()
                }
            }

            // Tags header: label + the add-tag field (keeps it off the tiny-item list).
            item {
                Column {
                    SectionLabel("Tags")
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
                            if (newTag.isNotBlank()) { vm.createTag(newTag); newTag = "" }
                        }) { Icon(Icons.Filled.Add, contentDescription = "Add tag") }
                    }
                }
            }

            items(tags, key = { it.id }) { tag ->
                var name by remember(tag.id) { mutableStateOf(tag.name) }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextFieldMMD(
                        value = name,
                        onValueChange = { name = it; vm.renameTag(tag, it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    IconButton(onClick = { vm.deleteTag(tag) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete tag")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    TextMMD(text, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp))
}
