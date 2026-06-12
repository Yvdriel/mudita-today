package com.mosquishe.today.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.bottom_sheet.ModalBottomSheetMMD
import com.mudita.mmd.components.bottom_sheet.rememberModalBottomSheetMMDState
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.time.TimeInputMMD
import com.mudita.mmd.components.time.rememberTimeInputMMDState
import java.time.LocalTime

/**
 * A bottom sheet wrapping the MMD time input. Returns the chosen time-of-day via [onResult] on Done.
 * Kept in a sheet (opened on demand) rather than inline, so the time field's auto-focus keyboard
 * only appears when the user actually wants to set a time. Used for to-do reminders and day-start.
 */
@Composable
fun TimeSheet(
    initial: LocalTime,
    onResult: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetMMDState(true) // skipPartiallyExpanded: open fully
    ModalBottomSheetMMD(onDismissRequest = onDismiss, sheetState = sheetState) {
        val state = rememberTimeInputMMDState(initial.hour, initial.minute, true)
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            TimeInputMMD(state, Modifier.padding(horizontal = 16.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButtonMMD(onClick = onDismiss) { TextMMD("Cancel") }
                Spacer(Modifier.width(8.dp))
                ButtonMMD(
                    onClick = {
                        onResult(LocalTime.of(state.hour, state.minute))
                        onDismiss()
                    },
                ) { TextMMD("Done") }
            }
        }
    }
}
