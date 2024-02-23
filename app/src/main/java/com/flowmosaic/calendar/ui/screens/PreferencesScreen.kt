package com.flowmosaic.calendar.ui.screens

import android.Manifest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.analytics.AgendaWidgetLogger
import com.flowmosaic.calendar.data.CalendarData
import com.flowmosaic.calendar.data.CalendarFetcher
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import com.flowmosaic.calendar.ui.dialog.ShowCalendarDialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

data class PreferenceSection(
    val title: String,
    val content: @Composable () -> Unit
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreferencesScreen(appWidgetId: Int) {
    val context = LocalContext.current
    val widgetId = if (appWidgetId != 0) appWidgetId.toString() else ""

    val sections = listOf(
        PreferenceSection(
            title = context.getString(R.string.prefs_title_general),
            content = { GeneralPrefsSection(widgetId) }
        ),
        PreferenceSection(
            title = context.getString(R.string.prefs_title_date_time),
            content = { DateAndTimePrefsSection(widgetId) }
        ),
        PreferenceSection(
            title = context.getString(R.string.prefs_title_appearance),
            content = { AppearancePrefsSection(widgetId) }
        ),
    )

    LazyColumn(Modifier.fillMaxSize(1f)) {
        sections.forEachIndexed { idx, section ->
            if (idx > 0) {
                item { Divider(color = MaterialTheme.colorScheme.outline, thickness = .5.dp) }
            }
            stickyHeader { TitleWithDivider(title = section.title) }
            item { section.content() }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GeneralPrefsSection(widgetId: String) {

    val context = LocalContext.current

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
    val numberOfDays = remember {
        mutableIntStateOf(AgendaWidgetPrefs.getNumberOfDays(context, widgetId))
    }
    val setNumberOfDays: (Int) -> Unit = { newValue ->
        numberOfDays.intValue = newValue
        AgendaWidgetPrefs.setNumberOfDays(context, newValue, widgetId)
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

    LaunchedEffect(key1 = Unit) {
        if (calendarPermissionsState.allPermissionsGranted) {
            calendarList.value = calendarFetcher.queryCalendarData(context)
            selectedCalendars.value =
                AgendaWidgetPrefs.getSelectedCalendars(context, calendarList.value, widgetId)
        }
    }

    if (showCalendarSelectionDialog.value) {
        ShowCalendarDialog(openDialog = showCalendarSelectionDialog, widgetId)
        AgendaWidgetLogger.logUpdatePrefEvent(
            context,
            AgendaWidgetLogger.PrefsScreenItemName.SELECT_CALENDARS
        )
    }

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
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun DateAndTimePrefsSection(widgetId: String) {
    val context = LocalContext.current

    val showEndTime = remember {
        mutableStateOf(AgendaWidgetPrefs.getShowEndTime(context, widgetId))
    }
    val setShowEndTime: (Boolean) -> Unit = { newValue ->
        showEndTime.value = newValue
        AgendaWidgetPrefs.setShowEndTime(context, newValue, widgetId)
    }
    val use12HourFormat = remember {
        mutableStateOf(AgendaWidgetPrefs.getHourFormat12(context, widgetId))
    }
    val setUse12HourFormat: (Boolean) -> Unit = { newValue ->
        use12HourFormat.value = newValue
        AgendaWidgetPrefs.setHourFormat12(context, newValue, widgetId)
    }

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
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun AppearancePrefsSection(widgetId: String) {
    val context = LocalContext.current

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

    val showDateSeparator = remember {
        mutableStateOf(AgendaWidgetPrefs.getSeparatorVisible(context, widgetId))
    }
    val setShowDateSeparator: (Boolean) -> Unit = { newValue ->
        showDateSeparator.value = newValue
        AgendaWidgetPrefs.setSeparatorVisible(context, newValue, widgetId)
    }

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
    Spacer(modifier = Modifier.height(16.dp))
}



