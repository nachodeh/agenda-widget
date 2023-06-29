package com.flowmosaic.calendar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    val showEndTime = remember {
        mutableStateOf(AgendaWidgetPrefs.getShowEndTime(context))
    }
    val coroutineScope = rememberCoroutineScope()

    val showDialog = remember {
        mutableStateOf(false)
    }

    if (showDialog.value) {
        ShowCalendarDialog(openDialog = showDialog)
    }

    LaunchedEffect(key1 = Unit) {
        calendarList.value = calendarFetcher.queryCalendarData(context)
        selectedCalendars.value =
            AgendaWidgetPrefs.getSelectedCalendars(context, calendarList.value)
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Agenda Widget", style = MaterialTheme.typography.headlineMedium)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    coroutineScope.launch {
                        showDialog.value = true
                    }
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Select Calendars", style = MaterialTheme.typography.bodyMedium)
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showEndTime.value = !showEndTime.value
                    AgendaWidgetPrefs.setShowEndTime(context, showEndTime.value)
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Show End Time", style = MaterialTheme.typography.bodyMedium)
            Checkbox(checked = showEndTime.value, onCheckedChange = null)
        }
    }
}