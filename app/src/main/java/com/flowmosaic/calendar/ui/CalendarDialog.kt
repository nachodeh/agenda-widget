package com.flowmosaic.calendar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.data.CalendarData
import com.flowmosaic.calendar.data.CalendarFetcher
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ShowCalendarDialog(openDialog: MutableState<Boolean>) {
    val context = LocalContext.current
    val calendarFetcher = CalendarFetcher()
    val calendarList = remember { mutableStateListOf<CalendarData>() }
    val selectedCalendars = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        calendarList.addAll(calendarFetcher.queryCalendarData(context))
        selectedCalendars.addAll(AgendaWidgetPrefs.getSelectedCalendars(context, calendarList))
    }

    val checkedItems = BooleanArray(calendarList.size) { index ->
        selectedCalendars.contains(calendarList[index].id.toString())
    }

    Dialog(
        onDismissRequest = {
            openDialog.value = false
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = {
            CalendarDialogContent(
                calendarList = calendarList,
                checkedItems = checkedItems,
                onCheckedChange = { index, isChecked ->
                    val calendarId = calendarList[index].id.toString()
                    if (isChecked) {
                        selectedCalendars.add(calendarId)
                    } else {
                        selectedCalendars.remove(calendarId)
                    }
                    checkedItems[index] = isChecked
                },
                onSaveClick = {
                    AgendaWidgetPrefs.setSelectedCalendars(context, selectedCalendars.toSet())
                    openDialog.value = false
                },
                onCancelClick = {
                    openDialog.value = false
                }
            )
        }
    )
}

@Composable
private fun CalendarDialogContent(
    calendarList: List<CalendarData>,
    checkedItems: BooleanArray,
    onCheckedChange: (Int, Boolean) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(16.dp)
            .wrapContentSize()
            .heightIn(min = 100.dp, max = 500.dp)
            .widthIn(min = 100.dp, max = 500.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(bottom = 80.dp) // Ensure there's enough space for the buttons
            ) {
                itemsIndexed(calendarList) { index, calendar ->
                    if (index < checkedItems.size) {
                        CalendarRow(
                            isChecked = checkedItems[index],
                            calendarName = calendar.name,
                            onCheckedChange = { isChecked ->
                                onCheckedChange(index, isChecked)
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onSaveClick) {
                    Text(text = stringResource(id = R.string.save),)
                }
                Button(
                    onClick = onCancelClick,
                    colors = ButtonDefaults.textButtonColors(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarRow(
    isChecked: Boolean,
    calendarName: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = {
                    onCheckedChange(!isChecked)
                }
            )
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = calendarName)
    }
}