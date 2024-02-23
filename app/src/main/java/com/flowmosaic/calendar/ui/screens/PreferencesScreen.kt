package com.flowmosaic.calendar.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.analytics.AgendaWidgetLogger
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs

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

@Composable
fun GeneralPrefsSection(widgetId: String) {
    val context = LocalContext.current

    val numberOfDays = remember {
        mutableIntStateOf(AgendaWidgetPrefs.getNumberOfDays(context, widgetId))
    }
    val showActionButtons = remember {
        mutableStateOf(AgendaWidgetPrefs.getShowActionButtons(context, widgetId))
    }
    val showNoUpcomingEvents = remember {
        mutableStateOf(AgendaWidgetPrefs.getShowNoUpcomingEventsText(context, widgetId))
    }

    SelectCalendarsButton(
        displayText = context.getString(R.string.select_calendars),
        widgetId = widgetId,
    )
    NumberSelectorRow(
        displayText = context.getString(R.string.number_of_days_to_display),
        numberValue = numberOfDays,
        saveNumberValue = { newValue: Int ->
            numberOfDays.intValue = newValue
            AgendaWidgetPrefs.setNumberOfDays(context, newValue, widgetId)
        }
    )
    CheckboxRow(
        displayText = context.getString(R.string.show_action_buttons),
        loggingItem = AgendaWidgetLogger.PrefsScreenItemName.SHOW_ACTION_BUTTONS,
        checkboxValue = showActionButtons,
        saveCheckboxValue = { newValue: Boolean ->
            showActionButtons.value = newValue
            AgendaWidgetPrefs.setShowActionButtons(context, newValue, widgetId)
        }
    )
    CheckboxRow(
        displayText = context.getString(R.string.show_no_upcoming_events),
        loggingItem = AgendaWidgetLogger.PrefsScreenItemName.SHOW_NO_EVENTS_TEXT,
        checkboxValue = showNoUpcomingEvents,
        saveCheckboxValue = { newValue: Boolean ->
            showNoUpcomingEvents.value = newValue
            AgendaWidgetPrefs.setShowNoUpcomingEventsText(context, newValue, widgetId)
        }
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun DateAndTimePrefsSection(widgetId: String) {
    val context = LocalContext.current

    val showEndTime = remember {
        mutableStateOf(AgendaWidgetPrefs.getShowEndTime(context, widgetId))
    }
    val use12HourFormat = remember {
        mutableStateOf(AgendaWidgetPrefs.getHourFormat12(context, widgetId))
    }

    CheckboxRow(
        displayText = context.getString(R.string.show_end_time),
        loggingItem = AgendaWidgetLogger.PrefsScreenItemName.SHOW_END_TIME,
        checkboxValue = showEndTime,
        saveCheckboxValue = { newValue: Boolean ->
            showEndTime.value = newValue
            AgendaWidgetPrefs.setShowEndTime(context, newValue, widgetId)
        }
    )
    CheckboxRow(
        displayText = context.getString(R.string.use_12_hour_format),
        loggingItem = AgendaWidgetLogger.PrefsScreenItemName.USE_12_HOUR,
        checkboxValue = use12HourFormat,
        saveCheckboxValue = { newValue: Boolean ->
            use12HourFormat.value = newValue
            AgendaWidgetPrefs.setHourFormat12(context, newValue, widgetId)
        }
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun AppearancePrefsSection(widgetId: String) {
    val context = LocalContext.current

    val colorState = remember { mutableStateOf(AgendaWidgetPrefs.getTextColor(context, widgetId)) }
    val fontSize = remember {
        mutableStateOf(AgendaWidgetPrefs.getFontSize(context, widgetId))
    }
    val textAlignment = remember {
        mutableStateOf(AgendaWidgetPrefs.getTextAlignment(context, widgetId))
    }
    val opacityState =
        remember { mutableFloatStateOf(AgendaWidgetPrefs.getOpacity(context, widgetId)) }

    val showDateSeparator = remember {
        mutableStateOf(AgendaWidgetPrefs.getSeparatorVisible(context, widgetId))
    }

    FontSizeSelectorRow(
        displayText = context.getString(R.string.font_size),
        fontSizeValue = fontSize,
        saveFontSizeValue = { newValue: AgendaWidgetPrefs.FontSize ->
            fontSize.value = newValue
            AgendaWidgetPrefs.setFontSize(context, newValue, widgetId)
        }
    )
    TextAlignmentSelectorRow(
        displayText = context.getString(R.string.text_alignment),
        textAlignmentValue = textAlignment,
        saveTextAlignmentValue = { newValue: AgendaWidgetPrefs.TextAlignment ->
            textAlignment.value = newValue
            AgendaWidgetPrefs.setTextAlignment(context, newValue, widgetId)
        },
    )
    ColorSelectorRow(
        displayText = context.getString(R.string.text_color),
        selectedColor = colorState,
        saveColorValue = { newValue: Color ->
            colorState.value = newValue
            AgendaWidgetPrefs.setTextColor(context, newValue, widgetId)
        }
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
        saveCheckboxValue = { newValue: Boolean ->
            showDateSeparator.value = newValue
            AgendaWidgetPrefs.setSeparatorVisible(context, newValue, widgetId)
        }
    )
    Spacer(modifier = Modifier.height(16.dp))
}



