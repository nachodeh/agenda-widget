package com.flowmosaic.calendar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flowmosaic.calendar.R
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
    val showCalendarSelectionDialog = rememberSaveable {
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
    val colorState = remember { mutableStateOf(AgendaWidgetPrefs.getTextColor(context)) }
    val setColorState: (Color) -> Unit = { newValue ->
        colorState.value = newValue
        AgendaWidgetPrefs.setTextColor(context, newValue)
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
        ButtonRow(
            displayText = context.getString(R.string.select_calendars),
            enableAction = showCalendarSelectionDialog
        )
        NumberSelectorRow(
            displayText = context.getString(R.string.number_of_days_to_display),
            numberValue = numberOfDays,
            saveNumberValue = setNumberOfDays
        )
        CheckboxRow(
            displayText = context.getString(R.string.show_end_time),
            checkboxValue = showEndTime,
            saveCheckboxValue = setShowEndTime
        )
        ColorSelectorRow(
            displayText = context.getString(R.string.text_color),
            selectedColor = colorState,
            saveColorValue = setColorState
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
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
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
        Checkbox(
            checked = checkboxValue.value, onCheckedChange = null, colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.primary
            )
        )
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
            .clickable { expanded.value = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = displayText, style = MaterialTheme.typography.bodyMedium)

        Box {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 0.dp)
            )
            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false },
                modifier = Modifier.background(
                    color = MaterialTheme.colorScheme.background,
                ),
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

@Composable
fun ColorSelectorRow(
    displayText: String,
    selectedColor: MutableState<Color>,
    saveColorValue: (Color) -> Unit
) {
    val showDialog = rememberSaveable {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog.value = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = displayText, style = MaterialTheme.typography.bodyMedium)
        Canvas(
            modifier = Modifier
                .clip(CircleShape)
                .border(
                    1.75.dp,
                    MaterialTheme.colorScheme.primary,
                    CircleShape
                )
                .background(selectedColor.value)
                .requiredSize(24.dp)
        ) {}
    }

    val colors = listOf(
        Color(0xFFFFFFFF),
        Color(0xFF000000),
        Color(0xFF888888),
        Color(0xFFEF9A9A),
        Color(0xFFF48FB1),
        Color(0xFF80CBC4),
        Color(0xFFA5D6A7),
        Color(0xFFFFCC80),
        Color(0xFFFFAB91),
        Color(0xFF81D4FA),
        Color(0xFFCE93D8),
        Color(0xFFB39DDB)
    )

    if (showDialog.value) {
        ColorDialog(
            colors,
            onDismiss = { showDialog.value = false },
            selectedColor.value,
            onColorSelected = saveColorValue
        )
    }
}


