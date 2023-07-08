package com.flowmosaic.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flowmosaic.calendar.data.CalendarData
import com.flowmosaic.calendar.data.CalendarFetcher
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import kotlinx.coroutines.launch

@Composable
fun PreferencesScreen() {
    val context = LocalContext.current
    val calendarFetcher = CalendarFetcher()
    val calendarList = remember { mutableStateOf(listOf<CalendarData>()) }
    val selectedCalendars = remember { mutableStateOf(setOf<String>()) }
    val showCalendarSelectionDialog = remember {
        mutableStateOf(false)
    }
    val showEndTime = remember {
        mutableStateOf(AgendaWidgetPrefs.getShowEndTime(context))
    }
    val setShowEndTime: (Boolean) -> Unit = { newValue ->
        showEndTime.value = newValue
        AgendaWidgetPrefs.setShowEndTime(context, newValue)
    }

    val numberOfDays = remember {
        mutableStateOf(AgendaWidgetPrefs.getNumberOfDays(context))
    }
    val setNumberOfDays: (Int) -> Unit = { newValue ->
        numberOfDays.value = newValue
        AgendaWidgetPrefs.setNumberOfDays(context, newValue)
    }

    if (showCalendarSelectionDialog.value) {
        ShowCalendarDialog(openDialog = showCalendarSelectionDialog)
    }

    LaunchedEffect(key1 = Unit) {
        calendarList.value = calendarFetcher.queryCalendarData(context)
        selectedCalendars.value =
            AgendaWidgetPrefs.getSelectedCalendars(context, calendarList.value)
    }

    Column {
        Header()
        ButtonRow(displayText = "Select calendars", enableAction = showCalendarSelectionDialog)
        CheckboxRow(
            displayText = "Show end time",
            checkboxValue = showEndTime,
            saveCheckboxValue = setShowEndTime
        )
        NumberSelectorRow(
            displayText = "Number of days to display",
            numberValue = numberOfDays,
            saveNumberValue = setNumberOfDays
        )
    }
}

@Composable
fun ButtonRow(displayText: String, enableAction: MutableState<Boolean>) {
    val coroutineScope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                coroutineScope.launch {
                    enableAction.value = true
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = displayText, style = MaterialTheme.typography.bodyMedium)
        Icon(Icons.Default.ArrowForward, contentDescription = null)
    }
}

@Composable
fun CheckboxRow(
    displayText: String,
    checkboxValue: MutableState<Boolean>,
    saveCheckboxValue: (Boolean) -> Unit
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable {
            checkboxValue.value = !checkboxValue.value
            saveCheckboxValue(checkboxValue.value)
        }
        .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = displayText, style = MaterialTheme.typography.bodyMedium)
        Checkbox(checked = checkboxValue.value, onCheckedChange = null)
    }
}

@Composable
fun NumberSelectorRow(
    displayText: String,
    numberValue: MutableState<Int>,
    saveNumberValue: (Int) -> Unit
) {
    val options = listOf(1, 3, 7, 10, 30)
    var expanded = remember { mutableStateOf(false) }
    val text = "${numberValue.value}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = displayText, style = MaterialTheme.typography.bodyMedium)

        Box {
            Text(text = text, modifier = Modifier.clickable { expanded.value = true })
            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        onClick = {
                            numberValue.value = option
                            saveNumberValue(option)
                            expanded.value = false
                        }, text = { Text(option.toString()) })
                }
            }
        }
    }
}


