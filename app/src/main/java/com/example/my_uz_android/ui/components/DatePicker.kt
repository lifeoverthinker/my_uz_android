package com.example.my_uz_android.ui.components

/**
 * Dialogowe komponenty wyboru daty i czasu oparte o Material 3.
 * Są wykorzystywane w formularzach dodawania i edycji danych wymagających
 * precyzyjnego wyboru terminu.
 */

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.example.my_uz_android.R

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Wyświetla dialog wyboru daty i zwraca wybraną wartość w milisekundach.
 *
 * @param date Aktualnie wybrana data w milisekundach lub null.
 * @param onDateSelected Callback wywoływany po potwierdzeniu wyboru daty.
 * @param onDismiss Callback zamykający dialog.
 */
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

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Wyświetla dialog wyboru godziny oparty o komponent TimePicker.
 *
 * @param initialHour Godzina początkowa.
 * @param initialMinute Minuta początkowa.
 * @param onTimeSelected Callback zwracający wybraną godzinę i minutę.
 * @param onDismiss Callback zamykający dialog.
 */
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