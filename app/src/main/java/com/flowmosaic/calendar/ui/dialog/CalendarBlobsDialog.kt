package com.flowmosaic.calendar.ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.analytics.AgendaWidgetLogger
import com.flowmosaic.calendar.data.CalendarData
import com.flowmosaic.calendar.data.CalendarFetcher
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import com.flowmosaic.calendar.ui.screens.ColorSelectorRow
import com.flowmosaic.calendar.ui.screens.IconSelectorRow

@Composable
fun ShowCalendarBlobsDialog(openDialog: MutableState<Boolean>, widgetId: String, logger: AgendaWidgetLogger) {
    val context = LocalContext.current
    val prefs = AgendaWidgetPrefs(context)

    val calendarFetcher = CalendarFetcher()
    val calendarList = remember { mutableStateListOf<CalendarData>() }
    LaunchedEffect(Unit) {
        calendarList.addAll(calendarFetcher.queryCalendarData(context))
    }

    var colors = IntArray(calendarList.size) { index ->
        prefs.getCalendarColor(widgetId, calendarList[index].id).toArgb()
    }

    var icons = IntArray(calendarList.size) { index ->
        prefs.getCalendarIcon(widgetId, calendarList[index].id)
    }

    Dialog(
        onDismissRequest = {
            openDialog.value = false
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = {
            CalendarBlobsDialogContent(
                calendarList = calendarList,
                colors = colors,
                onColorChange = { index, colorArgb ->
                    colors[index] = colorArgb
                },
                onSaveClick = {
                    colors.forEachIndexed{ index, colorArgb ->
                        val calendarId = calendarList[index].id
                        prefs.setCalendarColorArgb(colorArgb, widgetId, calendarId)
                    }
                    icons.forEachIndexed{ index, icon ->
                        val calendarId = calendarList[index].id
                        prefs.setCalendarIcon(icon, widgetId, calendarId)
                    }
                    openDialog.value = false
                },
                onCancelClick = {
                    openDialog.value = false
                },
                logger = logger,
                icons = icons,
                onIconChange = { index, icon ->
                    icons[index] = icon
                }
            )
        }
    )
}

@Composable
private fun CalendarBlobsDialogContent(
    calendarList: List<CalendarData>,
    colors: IntArray,
    onColorChange: (Int, Int) -> Unit,
    icons: IntArray,
    onIconChange: (Int, Int) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    logger: AgendaWidgetLogger
) {
    Surface(
        modifier = Modifier
            .padding(16.dp)
            .wrapContentSize()
            .heightIn(min = 100.dp, max = 500.dp)
            .widthIn(min = 100.dp, max = 500.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(bottom = 80.dp) // Ensure there's enough space for the buttons
            ) {
                itemsIndexed(calendarList) { index, calendar ->
                    if (index < colors.size) {
                        CalendarBlobRow(
                            calendarName = calendar.name,
                            colorArgb = colors[index],
                            onColorChange = { colorArgb ->
                                onColorChange(index, colorArgb)
                            },
                            icon = icons[index],
                            onIconChange = { icon ->
                                onIconChange(index, icon)
                            },
                            logger = logger
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
private fun CalendarBlobRow(
    calendarName: String,
    colorArgb: Int,
    onColorChange: (Int) -> Unit,
    icon: Int,
    onIconChange: (Int) -> Unit,
    logger: AgendaWidgetLogger
) {
    val context = LocalContext.current

    val colorState = remember { mutableStateOf(Color(colorArgb)) }
    var iconState = remember { mutableStateOf(icon) }

    Row() {
        Text(
            text = calendarName,
            fontWeight = FontWeight.Bold
        )
    }

    ColorSelectorRow(
        displayText = context.getString(R.string.title_color),
        selectedColor = colorState,
        saveColorValue = { newValue: Color ->
            colorState.value = newValue
            onColorChange(newValue.toArgb())
        },
        logger = logger,
        AgendaWidgetLogger.PrefsScreenItemName.CALENDAR_COLOR
    )

    IconSelectorRow(
        displayText = context.getString(R.string.title_icon),
        selectedIcon = iconState,
        saveIconValue = { newValue: Int ->
            iconState.value = newValue
            onIconChange(newValue)
        },
        logger = logger,
        prefName = AgendaWidgetLogger.PrefsScreenItemName.CALENDAR_ICON
    )

    HorizontalDivider(
        thickness = .5.dp,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}