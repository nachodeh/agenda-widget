package com.flowmosaic.calendar.ui

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
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.WidgetUtil
import com.flowmosaic.calendar.analytics.FirebaseLogger
import com.flowmosaic.calendar.data.CalendarData
import com.flowmosaic.calendar.data.CalendarFetcher
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import kotlinx.coroutines.launch

@Composable
fun PreferencesScreen() {
    val context = LocalContext.current
    var widgetId = remember { mutableStateOf("") }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val widgetUtil = WidgetUtil()
        val widgetIds = widgetUtil.getAllWidgetIds(context)
        widgetId.value = if (widgetIds.isEmpty()) "" else widgetIds[0].toString()
    }

    val calendarFetcher = CalendarFetcher()
    val calendarList = remember { mutableStateOf(listOf<CalendarData>()) }
    val selectedCalendars = remember { mutableStateOf(setOf<String>()) }
    val showCalendarSelectionDialog = rememberSaveable {
        mutableStateOf(false)
    }
    val showEndTime = remember {
        mutableStateOf(AgendaWidgetPrefs.getShowEndTime(context, widgetId.value))
    }
    val setShowEndTime: (Boolean) -> Unit = { newValue ->
        showEndTime.value = newValue
        AgendaWidgetPrefs.setShowEndTime(context, newValue, widgetId.value)
    }
    val numberOfDays = remember {
        mutableIntStateOf(AgendaWidgetPrefs.getNumberOfDays(context, widgetId.value))
    }
    val setNumberOfDays: (Int) -> Unit = { newValue ->
        numberOfDays.value = newValue
        AgendaWidgetPrefs.setNumberOfDays(context, newValue, widgetId.value)
    }
    val colorState = remember { mutableStateOf(AgendaWidgetPrefs.getTextColor(context, widgetId.value)) }
    val setColorState: (Color) -> Unit = { newValue ->
        colorState.value = newValue
        AgendaWidgetPrefs.setTextColor(context, newValue, widgetId.value)
    }

    val fontSize = remember {
        mutableStateOf(AgendaWidgetPrefs.getFontSize(context, widgetId.value))
    }
    val setFontSize: (AgendaWidgetPrefs.FontSize) -> Unit = { newValue ->
        fontSize.value = newValue
        AgendaWidgetPrefs.setFontSize(context, newValue, widgetId.value)
    }

    val textAlignment = remember {
        mutableStateOf(AgendaWidgetPrefs.getTextAlignment(context, widgetId.value))
    }
    val setTextAlignment: (AgendaWidgetPrefs.TextAlignment) -> Unit = { newValue ->
        textAlignment.value = newValue
        AgendaWidgetPrefs.setTextAlignment(context, newValue, widgetId.value)
    }

    val opacityState = remember { mutableFloatStateOf(AgendaWidgetPrefs.getOpacity(context, widgetId.value)) }

    val use12HourFormat = remember {
        mutableStateOf(AgendaWidgetPrefs.getHourFormat12(context, widgetId.value))
    }
    val setUse12HourFormat: (Boolean) -> Unit = { newValue ->
        use12HourFormat.value = newValue
        AgendaWidgetPrefs.setHourFormat12(context, newValue, widgetId.value)
    }

    val showDateSeparator = remember {
        mutableStateOf(AgendaWidgetPrefs.getSeparatorVisible(context, widgetId.value))
    }
    val setShowDateSeparator: (Boolean) -> Unit = { newValue ->
        showDateSeparator.value = newValue
        AgendaWidgetPrefs.setSeparatorVisible(context, newValue, widgetId.value)
    }

    if (showCalendarSelectionDialog.value) {
        ShowCalendarDialog(openDialog = showCalendarSelectionDialog, widgetId.value)
        FirebaseLogger.logSelectItemEvent(
            context,
            FirebaseLogger.ScreenName.PREFS,
            FirebaseLogger.PrefsScreenItemName.SELECT_CALENDARS.itemName
        )
    }

    LaunchedEffect(key1 = Unit) {
        calendarList.value = calendarFetcher.queryCalendarData(context)
        selectedCalendars.value =
            AgendaWidgetPrefs.getSelectedCalendars(context, calendarList.value, widgetId.value)
    }


    Column {
        Header()
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            TitleWithDivider(title = "General")
            ButtonRow(
                displayText = context.getString(R.string.select_calendars),
                enableAction = showCalendarSelectionDialog
            )
            NumberSelectorRow(
                displayText = context.getString(R.string.number_of_days_to_display),
                numberValue = numberOfDays,
                saveNumberValue = setNumberOfDays
            )
            TitleWithDivider(title = "Date and Time", spaceOnTop = true)
            CheckboxRow(
                displayText = context.getString(R.string.show_end_time),
                loggingItem = FirebaseLogger.PrefsScreenItemName.SHOW_END_TIME,
                checkboxValue = showEndTime,
                saveCheckboxValue = setShowEndTime
            )
            CheckboxRow(
                displayText = context.getString(R.string.use_12_hour_format),
                loggingItem = FirebaseLogger.PrefsScreenItemName.USE_12_HOUR,
                checkboxValue = use12HourFormat,
                saveCheckboxValue = setUse12HourFormat
            )
            TitleWithDivider(title = "Appearance", spaceOnTop = true)
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
                    AgendaWidgetPrefs.setOpacity(context, newValue, widgetId.value)
                }
            )
            CheckboxRow(
                displayText = context.getString(R.string.date_separator_visible),
                loggingItem = FirebaseLogger.PrefsScreenItemName.DATE_SEPARATOR,
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
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun CheckboxRow(
    displayText: String,
    loggingItem: FirebaseLogger.PrefsScreenItemName,
    checkboxValue: MutableState<Boolean>,
    saveCheckboxValue: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable {
            checkboxValue.value = !checkboxValue.value
            saveCheckboxValue(checkboxValue.value)
            FirebaseLogger.logSelectItemEvent(
                context,
                FirebaseLogger.ScreenName.PREFS,
                loggingItem.itemName,
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
                FirebaseLogger.logSelectItemEvent(
                    context,
                    FirebaseLogger.ScreenName.PREFS,
                    FirebaseLogger.PrefsScreenItemName.NUMBER_DAYS.itemName
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
                            FirebaseLogger.logSelectItemEvent(
                                context,
                                FirebaseLogger.ScreenName.PREFS,
                                FirebaseLogger.PrefsScreenItemName.FONT_SIZE.itemName
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
                            FirebaseLogger.logSelectItemEvent(
                                context,
                                FirebaseLogger.ScreenName.PREFS,
                                FirebaseLogger.PrefsScreenItemName.TEXT_ALIGNMENT.itemName
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
                FirebaseLogger.logSelectItemEvent(
                    context,
                    FirebaseLogger.ScreenName.PREFS,
                    FirebaseLogger.PrefsScreenItemName.TEXT_COLOR.itemName
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
            .padding(16.dp),
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
                FirebaseLogger.logSelectItemEvent(
                    context,
                    FirebaseLogger.ScreenName.PREFS,
                    FirebaseLogger.PrefsScreenItemName.OPACITY.itemName
                )
            },
            valueRange = 0f..1f,
            steps = 10,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun TitleWithDivider(title: String, spaceOnTop: Boolean = false) {
    if (spaceOnTop) {
        Spacer(modifier = Modifier.height(16.dp))
    }
    Divider(color = MaterialTheme.colorScheme.secondary, thickness = .5.dp)
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



