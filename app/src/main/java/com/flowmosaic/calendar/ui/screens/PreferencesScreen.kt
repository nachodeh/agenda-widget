package com.flowmosaic.calendar.ui.screens

import android.Manifest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
fun PreferencesScreen(appWidgetId: Int, onCloseClick: (() -> Unit)? = null) {
    val context = LocalContext.current
    val widgetId = if (appWidgetId != 0) appWidgetId.toString() else ""
    val logger = AgendaWidgetLogger(context)
    val prefs = AgendaWidgetPrefs(context)

    val sections = listOf(
        PreferenceSection(
            title = context.getString(R.string.prefs_title_general),
            content = { GeneralPrefsSection(widgetId, logger, prefs) }
        ),
        PreferenceSection(
            title = context.getString(R.string.prefs_title_date_time),
            content = { DateAndTimePrefsSection(widgetId, logger, prefs) }
        ),
        PreferenceSection(
            title = context.getString(R.string.prefs_title_appearance),
            content = { AppearancePrefsSection(widgetId, logger, prefs) }
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (onCloseClick != null) 80.dp else 0.dp) // Add padding at the bottom for the button
        ) {
            sections.forEachIndexed { idx, section ->
                if (idx > 0) {
                    item {
                        HorizontalDivider(
                            thickness = .5.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                stickyHeader { TitleWithDivider(title = section.title) }
                item {
                    section.content()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        onCloseClick?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { it() },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(text = context.getString(R.string.save))
                }
            }
        }
    }
}

@Composable
fun GeneralPrefsSection(widgetId: String, logger: AgendaWidgetLogger, prefs: AgendaWidgetPrefs) {
    val context = LocalContext.current

    val numberOfDays = remember {
        mutableIntStateOf(prefs.getNumberOfDays(widgetId))
    }
    val showActionButtons = remember {
        mutableStateOf(prefs.getShowActionButtons(widgetId))
    }
    val showNoUpcomingEvents = remember {
        mutableStateOf(prefs.getShowNoUpcomingEventsText(widgetId))
    }

    SelectCalendarsButton(
        displayText = context.getString(R.string.select_calendars),
        widgetId = widgetId,
        logger = logger,
        prefs = prefs
    )
    NumberSelectorRow(
        displayText = context.getString(R.string.number_of_days_to_display),
        numberValue = numberOfDays,
        saveNumberValue = { newValue: Int ->
            numberOfDays.intValue = newValue
            prefs.setNumberOfDays(newValue, widgetId)
        },
        logger = logger
    )
    CheckboxRow(
        displayText = context.getString(R.string.show_action_buttons),
        loggingItem = AgendaWidgetLogger.PrefsScreenItemName.SHOW_ACTION_BUTTONS,
        checkboxValue = showActionButtons,
        saveCheckboxValue = { newValue: Boolean ->
            showActionButtons.value = newValue
            prefs.setShowActionButtons(newValue, widgetId)
        },
        logger = logger
    )
    CheckboxRow(
        displayText = context.getString(R.string.show_no_upcoming_events),
        loggingItem = AgendaWidgetLogger.PrefsScreenItemName.SHOW_NO_EVENTS_TEXT,
        checkboxValue = showNoUpcomingEvents,
        saveCheckboxValue = { newValue: Boolean ->
            showNoUpcomingEvents.value = newValue
            prefs.setShowNoUpcomingEventsText(newValue, widgetId)
        },
        logger = logger
    )
}

@Composable
fun DateAndTimePrefsSection(
    widgetId: String,
    logger: AgendaWidgetLogger,
    prefs: AgendaWidgetPrefs
) {
    val context = LocalContext.current

    val showEndTime = remember {
        mutableStateOf(prefs.getShowEndTime(widgetId))
    }
    val use12HourFormat = remember {
        mutableStateOf(prefs.getHourFormat12(widgetId))
    }

    CheckboxRow(
        displayText = context.getString(R.string.show_end_time),
        loggingItem = AgendaWidgetLogger.PrefsScreenItemName.SHOW_END_TIME,
        checkboxValue = showEndTime,
        saveCheckboxValue = { newValue: Boolean ->
            showEndTime.value = newValue
            prefs.setShowEndTime(newValue, widgetId)
        },
        logger = logger
    )
    CheckboxRow(
        displayText = context.getString(R.string.use_12_hour_format),
        loggingItem = AgendaWidgetLogger.PrefsScreenItemName.USE_12_HOUR,
        checkboxValue = use12HourFormat,
        saveCheckboxValue = { newValue: Boolean ->
            use12HourFormat.value = newValue
            prefs.setHourFormat12(newValue, widgetId)
        },
        logger = logger
    )
}

@Composable
fun AppearancePrefsSection(widgetId: String, logger: AgendaWidgetLogger, prefs: AgendaWidgetPrefs) {
    val context = LocalContext.current

    val colorState = remember { mutableStateOf(prefs.getTextColor(widgetId)) }
    val fontSize = remember {
        mutableStateOf(prefs.getFontSize(widgetId))
    }
    val textAlignment = remember {
        mutableStateOf(prefs.getTextAlignment(widgetId))
    }
    val opacityState =
        remember { mutableFloatStateOf(prefs.getOpacity(widgetId)) }

    val showDateSeparator = remember {
        mutableStateOf(prefs.getSeparatorVisible(widgetId))
    }

    val alignBottom = remember {
        mutableStateOf(prefs.getAlignBottom(widgetId))
    }

    var showCalendarBlob = remember {
        mutableStateOf(prefs.getShowCalendarBlob(widgetId))
    }

    FontSizeSelectorRow(
        displayText = context.getString(R.string.font_size),
        fontSizeValue = fontSize,
        saveFontSizeValue = { newValue: AgendaWidgetPrefs.FontSize ->
            fontSize.value = newValue
            prefs.setFontSize(newValue, widgetId)
        },
        logger = logger
    )
    TextAlignmentSelectorRow(
        displayText = context.getString(R.string.text_alignment),
        textAlignmentValue = textAlignment,
        saveTextAlignmentValue = { newValue: AgendaWidgetPrefs.TextAlignment ->
            textAlignment.value = newValue
            prefs.setTextAlignment(newValue, widgetId)
        },
        logger = logger
    )
    ColorSelectorRow(
        displayText = context.getString(R.string.text_color),
        selectedColor = colorState,
        saveColorValue = { newValue: Color ->
            colorState.value = newValue
            prefs.setTextColor(newValue, widgetId)
        },
        logger = logger,
        AgendaWidgetLogger.PrefsScreenItemName.TEXT_COLOR
    )
    OpacitySelectorRow(
        displayText = context.getString(R.string.background_opacity),
        opacityValue = opacityState,
        saveOpacityValue = { newValue ->
            prefs.setOpacity(newValue, widgetId)
        },
        logger = logger
    )
    CheckboxRow(
        displayText = context.getString(R.string.date_separator_visible),
        loggingItem = AgendaWidgetLogger.PrefsScreenItemName.DATE_SEPARATOR,
        checkboxValue = showDateSeparator,
        saveCheckboxValue = { newValue: Boolean ->
            showDateSeparator.value = newValue
            prefs.setSeparatorVisible(newValue, widgetId)
        },
        logger = logger
    )
    CheckboxRow(
        displayText = context.getString(R.string.align_widget_bottom),
        loggingItem = AgendaWidgetLogger.PrefsScreenItemName.ALIGN_BOTTOM,
        checkboxValue = alignBottom,
        saveCheckboxValue = { newValue: Boolean ->
            alignBottom.value = newValue
            prefs.setAlignBottom(newValue, widgetId)
        },
        logger = logger
    )
    CheckboxRow(
        displayText = context.getString(R.string.show_calendar_blob),
        loggingItem = AgendaWidgetLogger.PrefsScreenItemName.SHOW_CALENDAR_BLOB,
        checkboxValue = showCalendarBlob,
        saveCheckboxValue = { newValue: Boolean ->
            showCalendarBlob.value = newValue
            prefs.setShowCalendarBlob(newValue, widgetId)
        },
        logger = logger
    )
    if (showCalendarBlob.value) {
        ConfigureCalendarBlobsButton(
            displayText = context.getString(R.string.configure_calendar_blobs),
            widgetId = widgetId,
            logger = logger
        )
    }
}