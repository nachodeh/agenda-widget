package com.flowmosaic.calendar.ui.screens

import android.Manifest
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.flowmosaic.calendar.analytics.AgendaWidgetLogger
import com.flowmosaic.calendar.data.CalendarData
import com.flowmosaic.calendar.data.CalendarFetcher
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import com.flowmosaic.calendar.ui.dialog.ColorDialog
import com.flowmosaic.calendar.ui.dialog.ShowCalendarDialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PreferencesScreen(appWidgetId: Int) {
    val context = LocalContext.current
    val widgetId = if (appWidgetId != 0) appWidgetId.toString() else ""

    val calendarPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.READ_CALENDAR,
        )
    )
    val calendarFetcher = CalendarFetcher()
    val calendarList = remember { mutableStateOf(listOf<CalendarData>()) }
    val selectedCalendars = remember { mutableStateOf(setOf<String>()) }
    val showCalendarSelectionDialog = rememberSaveable {
        mutableStateOf(false)
    }
    val showActionButtons = remember {
        mutableStateOf(AgendaWidgetPrefs.getShowActionButtons(context, widgetId))
    }
    val setShowActionButtons: (Boolean) -> Unit = { newValue ->
        showActionButtons.value = newValue
        AgendaWidgetPrefs.setShowActionButtons(context, newValue, widgetId)
    }

    val showNoUpcomingEvents = remember {
        mutableStateOf(AgendaWidgetPrefs.getShowNoUpcomingEventsText(context, widgetId))
    }
    val setShowNoUpcomingEvents: (Boolean) -> Unit = { newValue ->
        showNoUpcomingEvents.value = newValue
        AgendaWidgetPrefs.setShowNoUpcomingEventsText(context, newValue, widgetId)
    }

    val showEndTime = remember {
        mutableStateOf(AgendaWidgetPrefs.getShowEndTime(context, widgetId))
    }
    val setShowEndTime: (Boolean) -> Unit = { newValue ->
        showEndTime.value = newValue
        AgendaWidgetPrefs.setShowEndTime(context, newValue, widgetId)
    }
    val numberOfDays = remember {
        mutableIntStateOf(AgendaWidgetPrefs.getNumberOfDays(context, widgetId))
    }
    val setNumberOfDays: (Int) -> Unit = { newValue ->
        numberOfDays.intValue = newValue
        AgendaWidgetPrefs.setNumberOfDays(context, newValue, widgetId)
    }
    val colorState = remember { mutableStateOf(AgendaWidgetPrefs.getTextColor(context, widgetId)) }
    val setColorState: (Color) -> Unit = { newValue ->
        colorState.value = newValue
        AgendaWidgetPrefs.setTextColor(context, newValue, widgetId)
    }

    val fontSize = remember {
        mutableStateOf(AgendaWidgetPrefs.getFontSize(context, widgetId))
    }
    val setFontSize: (AgendaWidgetPrefs.FontSize) -> Unit = { newValue ->
        fontSize.value = newValue
        AgendaWidgetPrefs.setFontSize(context, newValue, widgetId)
    }

    val textAlignment = remember {
        mutableStateOf(AgendaWidgetPrefs.getTextAlignment(context, widgetId))
    }
    val setTextAlignment: (AgendaWidgetPrefs.TextAlignment) -> Unit = { newValue ->
        textAlignment.value = newValue
        AgendaWidgetPrefs.setTextAlignment(context, newValue, widgetId)
    }

    val opacityState =
        remember { mutableFloatStateOf(AgendaWidgetPrefs.getOpacity(context, widgetId)) }

    val use12HourFormat = remember {
        mutableStateOf(AgendaWidgetPrefs.getHourFormat12(context, widgetId))
    }
    val setUse12HourFormat: (Boolean) -> Unit = { newValue ->
        use12HourFormat.value = newValue
        AgendaWidgetPrefs.setHourFormat12(context, newValue, widgetId)
    }

    val showDateSeparator = remember {
        mutableStateOf(AgendaWidgetPrefs.getSeparatorVisible(context, widgetId))
    }
    val setShowDateSeparator: (Boolean) -> Unit = { newValue ->
        showDateSeparator.value = newValue
        AgendaWidgetPrefs.setSeparatorVisible(context, newValue, widgetId)
    }

    if (showCalendarSelectionDialog.value) {
        ShowCalendarDialog(openDialog = showCalendarSelectionDialog, widgetId)
        AgendaWidgetLogger.logUpdatePrefEvent(
            context,
            AgendaWidgetLogger.PrefsScreenItemName.SELECT_CALENDARS
        )
    }

    LaunchedEffect(key1 = Unit) {
        if (calendarPermissionsState.allPermissionsGranted) {
            calendarList.value = calendarFetcher.queryCalendarData(context)
            selectedCalendars.value =
                AgendaWidgetPrefs.getSelectedCalendars(context, calendarList.value, widgetId)
        }
    }

    Column {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            TitleWithDivider(
                title = context.getString(R.string.prefs_title_general),
                showDivider = false
            )
            if (calendarPermissionsState.allPermissionsGranted) {
                ButtonRow(
                    displayText = context.getString(R.string.select_calendars),
                    enableAction = showCalendarSelectionDialog
                )
            }
            NumberSelectorRow(
                displayText = context.getString(R.string.number_of_days_to_display),
                numberValue = numberOfDays,
                saveNumberValue = setNumberOfDays
            )
            CheckboxRow(
                displayText = context.getString(R.string.show_action_buttons),
                loggingItem = AgendaWidgetLogger.PrefsScreenItemName.SHOW_ACTION_BUTTONS,
                checkboxValue = showActionButtons,
                saveCheckboxValue = setShowActionButtons
            )
            CheckboxRow(
                displayText = context.getString(R.string.show_no_upcoming_events),
                loggingItem = AgendaWidgetLogger.PrefsScreenItemName.SHOW_NO_EVENTS_TEXT,
                checkboxValue = showNoUpcomingEvents,
                saveCheckboxValue = setShowNoUpcomingEvents
            )
            TitleWithDivider(
                title = context.getString(R.string.prefs_title_date_time),
                spaceOnTop = true
            )
            CheckboxRow(
                displayText = context.getString(R.string.show_end_time),
                loggingItem = AgendaWidgetLogger.PrefsScreenItemName.SHOW_END_TIME,
                checkboxValue = showEndTime,
                saveCheckboxValue = setShowEndTime
            )
            CheckboxRow(
                displayText = context.getString(R.string.use_12_hour_format),
                loggingItem = AgendaWidgetLogger.PrefsScreenItemName.USE_12_HOUR,
                checkboxValue = use12HourFormat,
                saveCheckboxValue = setUse12HourFormat
            )
            TitleWithDivider(
                title = context.getString(R.string.prefs_title_appearance),
                spaceOnTop = true
            )
            FontSizeSelectorRow(
                displayText = context.getString(R.string.font_size),
                fontSizeValue = fontSize,
                saveFontSizeValue = setFontSize
            )
            TextAlignmentSelectorRow(
                displayText = context.getString(R.string.text_alignment),
                textAlignmentValue = textAlignment,
                saveTextAlignmentValue = setTextAlignment,
            )
            ColorSelectorRow(
                displayText = context.getString(R.string.text_color),
                selectedColor = colorState,
                saveColorValue = setColorState
            )
            OpacitySelectorRow(
                displayText = context.getString(R.string.background_opacity),
                opacityValue = opacityState,
                saveOpacityValue = { newValue ->
                    AgendaWidgetPrefs.setOpacity(context, newValue, widgetId)
                }
            )
            CheckboxRow(
                displayText = context.getString(R.string.date_separator_visible),
                loggingItem = AgendaWidgetLogger.PrefsScreenItemName.DATE_SEPARATOR,
                checkboxValue = showDateSeparator,
                saveCheckboxValue = setShowDateSeparator
            )
        }
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
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun CheckboxRow(
    displayText: String,
    loggingItem: AgendaWidgetLogger.PrefsScreenItemName,
    checkboxValue: MutableState<Boolean>,
    saveCheckboxValue: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable {
            checkboxValue.value = !checkboxValue.value
            saveCheckboxValue(checkboxValue.value)
            AgendaWidgetLogger.logUpdatePrefEvent(
                context,
                loggingItem,
            )
        }
        .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = displayText, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checkboxValue.value, onCheckedChange = null)
    }
}

@Composable
fun NumberSelectorRow(
    displayText: String,
    numberValue: MutableState<Int>,
    saveNumberValue: (Int) -> Unit
) {
    val options = listOf(1, 2, 3, 7, 10, 30, 90, 365)
    val expanded = remember { mutableStateOf(false) }
    val text = "${numberValue.value}"
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                expanded.value = true
                AgendaWidgetLogger.logUpdatePrefEvent(
                    context,
                    AgendaWidgetLogger.PrefsScreenItemName.NUMBER_DAYS
                )
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = displayText, style = MaterialTheme.typography.bodyMedium)

        Box {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
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
fun FontSizeSelectorRow(
    displayText: String,
    fontSizeValue: MutableState<AgendaWidgetPrefs.FontSize>,
    saveFontSizeValue: (AgendaWidgetPrefs.FontSize) -> Unit
) {
    val options = AgendaWidgetPrefs.FontSize.values()
    val expanded = remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                expanded.value = true
                // Add your logging event here if necessary
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = displayText, style = MaterialTheme.typography.bodyMedium)

        Box {
            Text(
                text = fontSizeValue.value.getDisplayText(context),
                style = MaterialTheme.typography.bodyMedium,
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
                            fontSizeValue.value = option
                            saveFontSizeValue(option)
                            expanded.value = false
                            AgendaWidgetLogger.logUpdatePrefEvent(
                                context,
                                AgendaWidgetLogger.PrefsScreenItemName.FONT_SIZE
                            )
                        }, text = { Text(option.getDisplayText(context)) })
                }
            }
        }
    }
}

@Composable
fun TextAlignmentSelectorRow(
    displayText: String,
    textAlignmentValue: MutableState<AgendaWidgetPrefs.TextAlignment>,
    saveTextAlignmentValue: (AgendaWidgetPrefs.TextAlignment) -> Unit
) {
    val options = AgendaWidgetPrefs.TextAlignment.values()
    val expanded = remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                expanded.value = true
                // Add your logging event here if necessary
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = displayText, style = MaterialTheme.typography.bodyMedium)

        Box {
            Text(
                text = textAlignmentValue.value.getDisplayText(context),
                style = MaterialTheme.typography.bodyMedium,
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
                            textAlignmentValue.value = option
                            saveTextAlignmentValue(option)
                            expanded.value = false
                            AgendaWidgetLogger.logUpdatePrefEvent(
                                context,
                                AgendaWidgetLogger.PrefsScreenItemName.TEXT_ALIGNMENT
                            )
                        }, text = { Text(option.getDisplayText(context)) })
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
    val context = LocalContext.current
    val showDialog = rememberSaveable {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                showDialog.value = true
                AgendaWidgetLogger.logUpdatePrefEvent(
                    context,
                    AgendaWidgetLogger.PrefsScreenItemName.TEXT_COLOR
                )
            }
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
    saveOpacityValue: (Float) -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = displayText, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(32.dp)) // Adjust spacing as needed
        Slider(
            value = opacityValue.value,
            onValueChange = { newValue ->
                opacityValue.value = newValue
                saveOpacityValue(newValue)
            },
            onValueChangeFinished = {
                AgendaWidgetLogger.logUpdatePrefEvent(
                    context,
                    AgendaWidgetLogger.PrefsScreenItemName.OPACITY
                )
            },
            valueRange = 0f..1f,
            steps = 10,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun TitleWithDivider(title: String, spaceOnTop: Boolean = false, showDivider: Boolean = true) {
    if (spaceOnTop) {
        Spacer(modifier = Modifier.height(16.dp))
    }
    if (showDivider) {
        Divider(color = MaterialTheme.colorScheme.secondary, thickness = .5.dp)
    }
    Spacer(modifier = Modifier.height(8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ) // Adjust padding as needed
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
        )
    }
}



