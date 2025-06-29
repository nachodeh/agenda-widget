package com.flowmosaic.calendar.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.data.CalendarData
import com.flowmosaic.calendar.data.CalendarFetcher
import com.flowmosaic.calendar.data.CalendarPermissionsChecker
import java.util.concurrent.TimeUnit

class AgendaWidgetPrefs internal constructor(private val sharedPreferences: SharedPreferences) {

    constructor(context: Context) : this(
        context.getSharedPreferences(
            context.packageName + "_preferences",
            Context.MODE_PRIVATE
        )
    )

    companion object {
        private const val PREF_SELECTED_CALENDARS = "selected_calendars"
        private const val PREF_SHOW_ACTION_BUTTONS = "key_show_action_buttons"
        private const val PREF_SHOW_NO_UPCOMING_EVENTS = "key_show_no_upcoming_events"
        private const val PREF_SHOW_END_TIME = "key_show_end_time"
        private const val PREF_NUMBER_OF_DAYS = "key_number_of_days"
        private const val PREF_TEXT_COLOR = "key_text_color"
        private const val PREF_LAST_LOGGED = "lastLogged"
        private const val PREF_FONT_SIZE = "key_font_size"
        private const val PREF_TEXT_ALIGNMENT = "key_text_alignment"
        private const val PREF_OPACITY = "key_opacity"
        private const val PREF_HOUR_FORMAT_12 = "key_hour_format_12"
        private const val PREF_LAST_REVIEW_PROMPT = "key_last_review_prompt"
        private const val PREF_SEPARATOR_VISIBLE = "key_separator_visible"
        private const val PREF_ONBOARDING_DONE = "key_onboarding_done"
        private const val PREF_ALIGN_BOTTOM = "key_align_bottom"
        private const val PREF_SHOW_CALENDAR_BLOB = "show_calendar_blob"
        private const val PREF_CALENDAR_COLOR = "calendar_color"
    }

    enum class FontSize(private val displayResId: Int) {
        SMALL(R.string.font_size_small),
        MEDIUM(R.string.font_size_medium),
        LARGE(R.string.font_size_large);

        fun getDisplayText(context: Context): String {
            return context.getString(displayResId)
        }
    }

    enum class TextAlignment(private val displayResId: Int) {
        LEFT(R.string.text_alignment_left),
        CENTER(R.string.text_alignment_center),
        RIGHT(R.string.text_alignment_right);

        fun getDisplayText(context: Context): String {
            return context.getString(displayResId)
        }
    }

    fun getShouldLogWidgetActivityEvent(): Boolean {
        val lastLogged = sharedPreferences.getLong(PREF_LAST_LOGGED, 0)
        return (System.currentTimeMillis() - lastLogged) > TimeUnit.DAYS.toMillis(1)
    }

    fun setWidgetActivityEventLastLoggedTimestamp() {
        sharedPreferences.edit().putLong(PREF_LAST_LOGGED, System.currentTimeMillis()).apply()
    }

    suspend fun initSelectedCalendars(context: Context) {
        if (!sharedPreferences.contains(PREF_SELECTED_CALENDARS) && CalendarPermissionsChecker.hasCalendarPermission(
                context
            )
        ) {
            val allCalendars = CalendarFetcher().queryCalendarData(context)
            val allCalendarIds = allCalendars.map { it.id.toString() }.toMutableSet()
            setSelectedCalendars(allCalendarIds, widgetId = "")
        }
    }

    fun getSelectedCalendars(
        allCalendars: List<CalendarData>?,
        widgetId: String,
    ): MutableSet<String> {
        val (prefsKey, prefExists) = getKeyWithWidgetId(PREF_SELECTED_CALENDARS, widgetId)
        val selectedCalendars = sharedPreferences.getStringSet(prefsKey, null)

        val result = selectedCalendars?.toMutableSet()
            ?: if (allCalendars == null) {
                mutableSetOf()
            } else {
                // Selected calendars not found in preferences, save allCalendars as selected
                val allCalendarIds = allCalendars.map { it.id.toString() }.toMutableSet()
                setSelectedCalendars(allCalendarIds, widgetId)
                allCalendarIds
            }
        if (!prefExists) {
            setSelectedCalendars(result, widgetId)
        }

        return result
    }

    fun setSelectedCalendars(
        selectedCalendarIds: Set<String>,
        widgetId: String,
    ) {
        val prefsKey = getKeyWithWidgetIdSave(PREF_SELECTED_CALENDARS, widgetId)
        sharedPreferences.edit()
            .putStringSet(prefsKey, selectedCalendarIds)
            .apply()
    }

    fun getShowActionButtons(widgetId: String): Boolean {
        val (prefsKey, prefExists) = getKeyWithWidgetId(PREF_SHOW_ACTION_BUTTONS, widgetId)
        val result = sharedPreferences.getBoolean(prefsKey, true)
        if (!prefExists) {
            setShowActionButtons(result, widgetId)
        }
        return result
    }

    fun setShowActionButtons(showActionButtons: Boolean, widgetId: String) {
        val prefsKey = getKeyWithWidgetIdSave(PREF_SHOW_ACTION_BUTTONS, widgetId)
        sharedPreferences.edit().putBoolean(prefsKey, showActionButtons).apply()
    }

    fun getShowNoUpcomingEventsText(widgetId: String): Boolean {
        val (prefsKey, prefExists) = getKeyWithWidgetId(PREF_SHOW_NO_UPCOMING_EVENTS, widgetId)
        val result = sharedPreferences.getBoolean(prefsKey, true)
        if (!prefExists) {
            setShowNoUpcomingEventsText(result, widgetId)
        }
        return result
    }

    fun setShowNoUpcomingEventsText(showNoUpcomingEvents: Boolean, widgetId: String) {
        val prefsKey = getKeyWithWidgetIdSave(PREF_SHOW_NO_UPCOMING_EVENTS, widgetId)
        sharedPreferences.edit().putBoolean(prefsKey, showNoUpcomingEvents).apply()
    }

    fun getShowEndTime(widgetId: String): Boolean {
        val (prefsKey, prefExists) = getKeyWithWidgetId(PREF_SHOW_END_TIME, widgetId)
        val result = sharedPreferences.getBoolean(prefsKey, false)
        if (!prefExists) {
            setShowEndTime(result, widgetId)
        }
        return result
    }

    fun setShowEndTime(showEndTime: Boolean, widgetId: String) {
        val prefsKey = getKeyWithWidgetIdSave(PREF_SHOW_END_TIME, widgetId)
        sharedPreferences.edit().putBoolean(prefsKey, showEndTime).apply()
    }

    fun getNumberOfDays(widgetId: String): Int {
        val (prefsKey, prefExists) = getKeyWithWidgetId(PREF_NUMBER_OF_DAYS, widgetId)
        val result = sharedPreferences.getInt(prefsKey, 7)
        if (!prefExists) {
            setNumberOfDays(result, widgetId)
        }
        return result
    }

    fun setNumberOfDays(numberOfDays: Int, widgetId: String) {
        val prefsKey = getKeyWithWidgetIdSave(PREF_NUMBER_OF_DAYS, widgetId)
        sharedPreferences.edit().putInt(prefsKey, numberOfDays).apply()
    }

    fun getTextColor(widgetId: String): Color {
        val defaultColor = Color.White.toArgb()
        val (prefsKey, prefExists) = getKeyWithWidgetId(PREF_TEXT_COLOR, widgetId)
        val color = Color(sharedPreferences.getInt(prefsKey, defaultColor))
        if (!prefExists) {
            setTextColor(color, widgetId)
        }
        return color
    }

    fun setTextColor(textColor: Color, widgetId: String) {
        val colorInt = textColor.toArgb()
        val prefsKey = getKeyWithWidgetIdSave(PREF_TEXT_COLOR, widgetId)
        sharedPreferences.edit().putInt(prefsKey, colorInt).apply()
    }

    fun getFontSize(widgetId: String): FontSize {
        val (prefsKey, prefExists) = getKeyWithWidgetId(PREF_FONT_SIZE, widgetId)
        val size = sharedPreferences.getString(prefsKey, FontSize.MEDIUM.name)
            ?: FontSize.MEDIUM.name
        val fontSize = FontSize.valueOf(size)
        if (!prefExists) {
            setFontSize(fontSize, widgetId)
        }
        return fontSize
    }

    fun setFontSize(fontSize: FontSize, widgetId: String) {
        val prefsKey = getKeyWithWidgetIdSave(PREF_FONT_SIZE, widgetId)
        sharedPreferences.edit().putString(prefsKey, fontSize.name).apply()
    }

    fun getTextAlignment(widgetId: String): TextAlignment {
        val (prefsKey, prefExists) = getKeyWithWidgetId(PREF_TEXT_ALIGNMENT, widgetId)
        val alignment = sharedPreferences.getString(prefsKey, TextAlignment.LEFT.name)
            ?: TextAlignment.LEFT.name
        val textAlignment = TextAlignment.valueOf(alignment)
        if (!prefExists) {
            setTextAlignment(textAlignment, widgetId)
        }
        return textAlignment
    }

    fun setTextAlignment(textAlignment: TextAlignment, widgetId: String) {
        val prefsKey = getKeyWithWidgetIdSave(PREF_TEXT_ALIGNMENT, widgetId)
        sharedPreferences.edit().putString(prefsKey, textAlignment.name).apply()
    }

    fun getOpacity(widgetId: String): Float {
        val (prefsKey, prefExists) = getKeyWithWidgetId(PREF_OPACITY, widgetId)
        val opacity = sharedPreferences.getFloat(prefsKey, 0.0f)
        if (!prefExists) {
            setOpacity(opacity, widgetId)
        }
        return opacity
    }

    fun setOpacity(opacity: Float, widgetId: String) {
        val prefsKey = getKeyWithWidgetIdSave(PREF_OPACITY, widgetId)
        sharedPreferences.edit().putFloat(prefsKey, opacity).apply()
    }

    fun getHourFormat12(widgetId: String): Boolean {
        val (prefsKey, prefExists) = getKeyWithWidgetId(PREF_HOUR_FORMAT_12, widgetId)
        val hourFormat = sharedPreferences.getBoolean(prefsKey, false)
        if (!prefExists) {
            setHourFormat12(hourFormat, widgetId)
        }
        return hourFormat
    }

    fun setHourFormat12(use12HourFormat: Boolean, widgetId: String) {
        val prefsKey = getKeyWithWidgetIdSave(PREF_HOUR_FORMAT_12, widgetId)
        sharedPreferences.edit().putBoolean(prefsKey, use12HourFormat).apply()
    }

    fun setSeparatorVisible(visible: Boolean, widgetId: String) {
        val prefsKey = getKeyWithWidgetIdSave(PREF_SEPARATOR_VISIBLE, widgetId)
        sharedPreferences.edit().putBoolean(prefsKey, visible).apply()
    }

    fun getSeparatorVisible(widgetId: String): Boolean {
        val (prefsKey, prefExists) = getKeyWithWidgetId(PREF_SEPARATOR_VISIBLE, widgetId)
        val separatorVisible = sharedPreferences.getBoolean(prefsKey, false)
        if (!prefExists) {
            setSeparatorVisible(separatorVisible, widgetId)
        }
        return separatorVisible
    }

    fun setAlignBottom(alignBottom: Boolean, widgetId: String) {
        val prefsKey = getKeyWithWidgetIdSave(PREF_ALIGN_BOTTOM, widgetId)
        sharedPreferences.edit().putBoolean(prefsKey, alignBottom).apply()
    }

    fun getAlignBottom(widgetId: String): Boolean {
        val (prefsKey, prefExists) = getKeyWithWidgetId(PREF_ALIGN_BOTTOM, widgetId)
        val alignBottom = sharedPreferences.getBoolean(prefsKey, false)
        if (!prefExists) {
            setAlignBottom(alignBottom, widgetId)
        }
        return alignBottom
    }

    fun getLastReviewPrompt(): Long {
        return sharedPreferences.getLong(PREF_LAST_REVIEW_PROMPT, 0)
    }

    fun setLastReviewPrompt(lastReviewPrompt: Long) {
        sharedPreferences.edit().putLong(PREF_LAST_REVIEW_PROMPT, lastReviewPrompt).apply()
    }

    fun getOnboardingDone(): Boolean {
        return sharedPreferences.getBoolean(PREF_ONBOARDING_DONE, false)
    }

    fun setOnboardingDone(onboardingDone: Boolean) {
        sharedPreferences.edit().putBoolean(PREF_ONBOARDING_DONE, onboardingDone).apply()
    }

    private fun getKeyWithWidgetId(
        key: String,
        widgetId: String
    ): Pair<String, Boolean> {
        if (widgetId.isEmpty()) {
            return key to true
        }

        val keyWithId = key.plus("_").plus(widgetId)
        if (sharedPreferences.contains(keyWithId) || !sharedPreferences.contains(key)) {
            return keyWithId to true
        }
        return key to false
    }

    private fun getKeyWithWidgetIdSave(key: String, widgetId: String): String {
        if (widgetId.isEmpty()) {
            return key
        }

        return key.plus("_").plus(widgetId)
    }

    fun getShowCalendarBlob(widgetId: String): Boolean {
        val (prefsKey, prefExists) = getKeyWithWidgetId(PREF_SHOW_CALENDAR_BLOB, widgetId)
        val showCalendarBlob = sharedPreferences.getBoolean(prefsKey, false)
        if (!prefExists) {
            setShowCalendarBlob(showCalendarBlob, widgetId)
        }
        return showCalendarBlob
    }

    fun setShowCalendarBlob(showCalendarBlob: Boolean, widgetId: String) {
        val prefsKey = getKeyWithWidgetIdSave(PREF_SHOW_CALENDAR_BLOB, widgetId)
        sharedPreferences.edit().putBoolean(prefsKey, showCalendarBlob).apply()
    }

    private fun getCalendarColorPrefKey(calendarId: Long): String {
        return PREF_CALENDAR_COLOR.plus("#").plus(calendarId.toString())
    }

    fun getCalendarColor(widgetId: String, calendarId: Long): Color {
        val defaultColor = Color.White.toArgb()
        val (prefsKey, prefExists) = getKeyWithWidgetId(getCalendarColorPrefKey(calendarId), widgetId)
        val color = Color(sharedPreferences.getInt(prefsKey, defaultColor))
        if (!prefExists) {
            setCalendarColor(color, widgetId, calendarId)
        }
        return color
    }

    fun setCalendarColor(calendarColor: Color, widgetId: String, calendarId: Long) {
        val colorArgb = calendarColor.toArgb()
        setCalendarColorArgb(colorArgb, widgetId, calendarId)
    }

    fun setCalendarColorArgb(calendarColorArgb: Int, widgetId: String, calendarId: Long) {
        val prefsKey = getKeyWithWidgetIdSave(getCalendarColorPrefKey(calendarId), widgetId)
        sharedPreferences.edit().putInt(prefsKey, calendarColorArgb).apply()
    }
}