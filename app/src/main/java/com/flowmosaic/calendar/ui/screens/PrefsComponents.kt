package com.flowmosaic.calendar.ui.screens

import android.Manifest
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flowmosaic.calendar.analytics.AgendaWidgetLogger
import com.flowmosaic.calendar.data.CalendarData
import com.flowmosaic.calendar.data.CalendarFetcher
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import com.flowmosaic.calendar.ui.dialog.ColorDialog
import com.flowmosaic.calendar.ui.dialog.IconDialog
import com.flowmosaic.calendar.ui.dialog.ShowCalendarBlobsDialog
import com.flowmosaic.calendar.ui.dialog.ShowCalendarDialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import com.flowmosaic.calendar.R

@Composable
fun TitleWithDivider(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 16.dp)
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SelectCalendarsButton(displayText: String, widgetId: String, logger: AgendaWidgetLogger, prefs: AgendaWidgetPrefs) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val showCalendarSelectionDialog = rememberSaveable {
        mutableStateOf(false)
    }

    val calendarFetcher = CalendarFetcher()
    val calendarList = remember { mutableStateOf(listOf<CalendarData>()) }
    val selectedCalendars = remember { mutableStateOf(setOf<String>()) }

    val calendarPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.READ_CALENDAR,
        )
    )

    LaunchedEffect(key1 = Unit) {
        if (calendarPermissionsState.allPermissionsGranted) {
            calendarList.value = calendarFetcher.queryCalendarData(context)
            selectedCalendars.value =
                prefs.getSelectedCalendars(calendarList.value, widgetId)
        }
    }

    if (!calendarPermissionsState.allPermissionsGranted) {
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                coroutineScope.launch {
                    showCalendarSelectionDialog.value = true
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }

    if (showCalendarSelectionDialog.value) {
        ShowCalendarDialog(openDialog = showCalendarSelectionDialog, widgetId)
        logger.logUpdatePrefEvent(
            AgendaWidgetLogger.PrefsScreenItemName.SELECT_CALENDARS
        )
    }
}

@Composable
fun CheckboxRow(
    displayText: String,
    loggingItem: AgendaWidgetLogger.PrefsScreenItemName,
    checkboxValue: MutableState<Boolean>,
    saveCheckboxValue: (Boolean) -> Unit,
    logger: AgendaWidgetLogger
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable {
            checkboxValue.value = !checkboxValue.value
            saveCheckboxValue(checkboxValue.value)
            logger.logUpdatePrefEvent(loggingItem)
        }
        .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checkboxValue.value, onCheckedChange = null)
    }
}

@Composable
fun NumberSelectorRow(
    displayText: String,
    numberValue: MutableState<Int>,
    saveNumberValue: (Int) -> Unit,
    logger: AgendaWidgetLogger
) {
    val options = arrayOf(1, 2, 3, 7, 10, 30, 90, 365)

    MultipleOptionsSelectorRow(
        displayText = displayText,
        selectedValue = numberValue,
        options = options,
        saveSelectedValue = saveNumberValue,
        getDisplayText = { option, _ -> option.toString() },
        loggingItem = AgendaWidgetLogger.PrefsScreenItemName.NUMBER_DAYS,
        logger = logger
    )
}

@Composable
fun FontSizeSelectorRow(
    displayText: String,
    fontSizeValue: MutableState<AgendaWidgetPrefs.FontSize>,
    saveFontSizeValue: (AgendaWidgetPrefs.FontSize) -> Unit,
    logger: AgendaWidgetLogger
) {
    MultipleOptionsSelectorRow(
        displayText = displayText,
        selectedValue = fontSizeValue,
        options = AgendaWidgetPrefs.FontSize.entries.toTypedArray(),
        saveSelectedValue = saveFontSizeValue,
        getDisplayText = { option, context -> option.getDisplayText(context) },
        loggingItem = AgendaWidgetLogger.PrefsScreenItemName.FONT_SIZE,
        logger = logger
    )
}

@Composable
fun TextAlignmentSelectorRow(
    displayText: String,
    textAlignmentValue: MutableState<AgendaWidgetPrefs.TextAlignment>,
    saveTextAlignmentValue: (AgendaWidgetPrefs.TextAlignment) -> Unit,
    logger: AgendaWidgetLogger
) {
    MultipleOptionsSelectorRow(
        displayText = displayText,
        selectedValue = textAlignmentValue,
        options = AgendaWidgetPrefs.TextAlignment.entries.toTypedArray(),
        saveSelectedValue = saveTextAlignmentValue,
        getDisplayText = { option, context -> option.getDisplayText(context) },
        loggingItem = AgendaWidgetLogger.PrefsScreenItemName.TEXT_ALIGNMENT,
        logger = logger,
    )
}

@Composable
fun <T> MultipleOptionsSelectorRow(
    displayText: String,
    selectedValue: MutableState<T>,
    options: Array<T>,
    saveSelectedValue: (T) -> Unit,
    getDisplayText: (T, Context) -> String,
    loggingItem: AgendaWidgetLogger.PrefsScreenItemName,
    logger: AgendaWidgetLogger
) {
    val expanded = remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded.value = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Box {
            Text(
                text = getDisplayText(selectedValue.value, context),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 0.dp)
            )
            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false },
                modifier = Modifier.background(color = MaterialTheme.colorScheme.background)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        onClick = {
                            selectedValue.value = option
                            saveSelectedValue(option)
                            expanded.value = false
                            logger.logUpdatePrefEvent(loggingItem)
                        },
                        text = { Text(getDisplayText(option, context)) }
                    )
                }
            }
        }
    }
}

@Composable
fun ColorSelectorRow(
    displayText: String,
    selectedColor: MutableState<Color>,
    saveColorValue: (Color) -> Unit,
    logger: AgendaWidgetLogger,
    prefName: AgendaWidgetLogger.PrefsScreenItemName
) {
    val showDialog = rememberSaveable {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                showDialog.value = true
                logger.logUpdatePrefEvent(prefName)
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
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
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.inversePrimary,
        Color(0xFF80CBC4),
        Color(0xFFFFCC80),
        Color(0xFFFFAB91),
        Color(0xFF81D4FA),
        Color(0xFFB39DDB),
        Color(0xFF000000),
//        Color(0xFF888888),
//        Color(0xFFEF9A9A),
//        Color(0xFFF48FB1),
//        Color(0xFFA5D6A7),
//        Color(0xFFCE93D8),
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

@Composable
fun OpacitySelectorRow(
    displayText: String,
    opacityValue: MutableState<Float>,
    saveOpacityValue: (Float) -> Unit,
    logger: AgendaWidgetLogger
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(32.dp)) // Adjust spacing as needed
        Slider(
            value = opacityValue.value,
            onValueChange = { newValue ->
                opacityValue.value = newValue
                saveOpacityValue(newValue)
            },
            onValueChangeFinished = {
                logger.logUpdatePrefEvent(
                    AgendaWidgetLogger.PrefsScreenItemName.OPACITY
                )
            },
            valueRange = 0f..1f,
            steps = 10,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ConfigureCalendarBlobsButton(displayText: String, widgetId: String, logger: AgendaWidgetLogger) {
    val coroutineScope = rememberCoroutineScope()

    val showConfigureCalendarBlobsDialog = rememberSaveable {
        mutableStateOf(false)
    }

    val calendarPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.READ_CALENDAR,
        )
    )

    if (!calendarPermissionsState.allPermissionsGranted) {
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                coroutineScope.launch {
                    showConfigureCalendarBlobsDialog.value = true
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }

    if (showConfigureCalendarBlobsDialog.value) {
        ShowCalendarBlobsDialog(openDialog = showConfigureCalendarBlobsDialog, widgetId, logger)
        logger.logUpdatePrefEvent(
            AgendaWidgetLogger.PrefsScreenItemName.CONFIGURE_CALENDAR_BLOBS
        )
    }
}

@Composable
fun IconSelectorRow(
    displayText: String,
    selectedIcon: MutableState<Int>,
    saveIconValue: (Int) -> Unit,
    logger: AgendaWidgetLogger,
    prefName: AgendaWidgetLogger.PrefsScreenItemName
) {
    val showDialog = rememberSaveable {
        mutableStateOf(false)
    }

    val icons = listOf(
        R.drawable.close,
        R.drawable.airplane,
        R.drawable.alarm,
        R.drawable.bank,
        R.drawable.baseball,
        R.drawable.cake,
        R.drawable.calendar,
        R.drawable.car,
        R.drawable.cycling,
        R.drawable.football,
        R.drawable.gaming,
        R.drawable.gavel,
        R.drawable.holiday,
        R.drawable.messages,
        R.drawable.music,
        R.drawable.pram,
        R.drawable.present,
        R.drawable.soccer,
        R.drawable.suitcase,
        R.drawable.ticket,
        R.drawable.train,
        R.drawable.university,
        R.drawable.wallet,
        R.drawable.weather,
        R.drawable.weights,
    )

    if (selectedIcon.value < 0 || selectedIcon.value >= icons.size) {
        selectedIcon.value = 0
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                showDialog.value = true
                logger.logUpdatePrefEvent(prefName)
            }
            .padding(bottom = 16.dp)
            .padding(start = 16.dp)
            .padding(end = 16.dp),

        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = ImageVector.vectorResource(icons[selectedIcon.value]),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(24.dp).height(24.dp)
        )
    }

    if (showDialog.value) {
        IconDialog(
            iconList = icons,
            onDismiss = { showDialog.value = false },
            selectedIcon = selectedIcon.value,
            onIconSelected = saveIconValue
        )
    }
}