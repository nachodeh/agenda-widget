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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.analytics.AgendaWidgetLogger
import com.flowmosaic.calendar.data.CalendarData
import com.flowmosaic.calendar.data.CalendarFetcher
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import com.flowmosaic.calendar.ui.screens.ColorSelectorRow

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
                    openDialog.value = false
                },
                onCancelClick = {
                    openDialog.value = false
                },
                logger = logger
            )
        }
    )
}

@Composable
private fun CalendarBlobsDialogContent(
    calendarList: List<CalendarData>,
    colors: IntArray,
    onColorChange: (Int, Int) -> Unit,
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
                            colorArgb = colors[index],
                            calendarName = calendar.name,
                            onColorChange = { colorArgb ->
                                onColorChange(index, colorArgb)
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
    colorArgb: Int,
    calendarName: String,
    onColorChange: (Int) -> Unit,
    logger: AgendaWidgetLogger
) {
    val context = LocalContext.current

    val colorState = remember { mutableStateOf(Color(colorArgb)) }

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
        logger = logger
    )

    HorizontalDivider(
        thickness = .5.dp,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}