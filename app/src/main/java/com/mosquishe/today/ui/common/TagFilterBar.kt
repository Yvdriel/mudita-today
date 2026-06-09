package com.mosquishe.today.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mosquishe.today.data.local.TagEntity
import androidx.compose.foundation.lazy.LazyRow
import com.mudita.mmd.components.chips.FilterChipMMD
import com.mudita.mmd.components.text.TextMMD

/** Horizontal "All + tags" filter chip row. Hidden by the caller when there are no tags. */
@Composable
fun TagFilterBar(
    tags: List<TagEntity>,
    selectedTagId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        item {
            FilterChipMMD(
                selected = selectedTagId == null,
                onClick = { onSelect(null) },
                label = { TextMMD("All") },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        items(tags.size) { i ->
            val tag = tags[i]
            FilterChipMMD(
                selected = selectedTagId == tag.id,
                onClick = { onSelect(tag.id) },
                label = { TextMMD(tag.name) },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}
