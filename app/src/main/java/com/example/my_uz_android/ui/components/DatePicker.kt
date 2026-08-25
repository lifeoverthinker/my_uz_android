package com.example.my_uz_android.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.example.my_uz_android.R

// Dialog wyboru daty z kalendarza
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePicker(
    date: Long?,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val safeInitialDateMillis = date?.takeIf { it > 0L } ?: System.currentTimeMillis()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = safeInitialDateMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { onDateSelected(it) }
            }) {
                Text(stringResource(R.string.btn_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        androidx.compose.material3.DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )
    }
}

// Dialog zegara do wyboru godziny i minut
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePicker(
    initialHour: Int = 12,
    initialMinute: Int = 0,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        confirmButton = {
            TextButton(onClick = { onTimeSelected(timePickerState.hour, timePickerState.minute) }) {
                Text(stringResource(R.string.btn_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        text = {
            androidx.compose.material3.TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    )
}