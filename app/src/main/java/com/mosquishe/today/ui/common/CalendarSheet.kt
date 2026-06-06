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
import com.mosquishe.today.util.Dates
import com.mudita.mmd.components.bottom_sheet.ModalBottomSheetMMD
import com.mudita.mmd.components.bottom_sheet.rememberModalBottomSheetMMDState
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.time.DatePickerMMD
import com.mudita.mmd.components.time.rememberDatePickerMMDState
import java.time.LocalDate

/** A bottom sheet wrapping the MMD calendar. Returns the chosen date via [onResult] on Done. */
@Composable
fun CalendarSheet(
    initial: LocalDate?,
    onResult: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetMMDState()
    ModalBottomSheetMMD(onDismissRequest = onDismiss, sheetState = sheetState) {
        val state = rememberDatePickerMMDState(
            initialSelectedDateMillis = initial?.let { Dates.localDateToUtcMillis(it) },
        )
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            DatePickerMMD(state = state, modifier = Modifier.fillMaxWidth())
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButtonMMD(onClick = onDismiss) { TextMMD("Cancel") }
                Spacer(Modifier.width(8.dp))
                ButtonMMD(
                    onClick = {
                        state.selectedDateMillis?.let { onResult(Dates.utcMillisToLocalDate(it)) }
                        onDismiss()
                    },
                ) { TextMMD("Done") }
            }
        }
    }
}
