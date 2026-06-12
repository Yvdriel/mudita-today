package com.mosquishe.today.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mosquishe.today.data.local.TagEntity
import com.mudita.mmd.components.chips.FilterChipMMD
import com.mudita.mmd.components.text.TextMMD

/**
 * "All + tags" filter chips. A FlowRow (wraps to the next line), not a LazyRowMMD: a short chip
 * row needs no scrollbar, and two MMD lazy lists in one Column (this plus the task LazyColumnMMD)
 * leave the list blank. The MMD docs say to keep short horizontal rows as a plain Row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagFilterBar(
    tags: List<TagEntity>,
    selectedTagId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChipMMD(
            selected = selectedTagId == null,
            onClick = { onSelect(null) },
            label = { TextMMD("All") },
        )
        tags.forEach { tag ->
            FilterChipMMD(
                selected = selectedTagId == tag.id,
                onClick = { onSelect(tag.id) },
                label = { TextMMD(tag.name) },
            )
        }
    }
}
