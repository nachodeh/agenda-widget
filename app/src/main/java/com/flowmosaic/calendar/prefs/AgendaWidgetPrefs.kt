package com.flowmosaic.calendar.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.data.CalendarData
import java.util.concurrent.TimeUnit

object AgendaWidgetPrefs {

    private const val PREF_SELECTED_CALENDARS = "selected_calendars"
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

    fun getShouldLogWidgetActivityEvent(context: Context): Boolean {
        val lastLogged = getPreferences(context).getLong(PREF_LAST_LOGGED, 0)
        return (System.currentTimeMillis() - lastLogged) > TimeUnit.DAYS.toMillis(1)
    }

    fun setWidgetActivityEventLastLoggedTimestamp(context: Context) {
        getPreferences(context).edit().putLong(PREF_LAST_LOGGED, System.currentTimeMillis()).apply()
    }

    fun getKeyWithWidgetId(context: Context, key: String, widgetId: String): String {
        if (widgetId.isEmpty()) {
            return key
        }

        val keyWithId = key.plus("_").plus(widgetId)
        if (getPreferences(context).contains(keyWithId) || !getPreferences(context).contains(key)) {
            return keyWithId
        }
        return key
    }

    fun getSelectedCalendars(
        context: Context,
        allCalendars: List<CalendarData>?,
        widgetId: String,
    ): MutableSet<String> {
        val prefsKey = getKeyWithWidgetId(context, PREF_SELECTED_CALENDARS, widgetId)
        val selectedCalendars = getPreferences(context).getStringSet(prefsKey, null)

        return selectedCalendars?.toMutableSet()
            ?: if (allCalendars == null) {
                mutableSetOf()
            } else {
                // Selected calendars not found in preferences, save allCalendars as selected
                val allCalendarIds = allCalendars.map { it.id.toString() }.toMutableSet()
                setSelectedCalendars(context, allCalendarIds, widgetId)
                allCalendarIds
            }
    }

    fun setSelectedCalendars(context: Context, selectedCalendarIds: Set<String>, widgetId: String,) {
        val prefsKey = getKeyWithWidgetId(context, PREF_SELECTED_CALENDARS, widgetId)
        getPreferences(context).edit()
            .putStringSet(prefsKey, selectedCalendarIds)
            .apply()
    }

    fun getShowEndTime(context: Context, widgetId: String,): Boolean {
        val prefsKey = getKeyWithWidgetId(context, PREF_SHOW_END_TIME, widgetId)
        return getPreferences(context).getBoolean(prefsKey, false)
    }

    fun setShowEndTime(context: Context, showEndTime: Boolean, widgetId: String,) {
        val prefsKey = getKeyWithWidgetId(context, PREF_SHOW_END_TIME, widgetId)
        getPreferences(context).edit().putBoolean(prefsKey, showEndTime).apply()
    }

    fun getNumberOfDays(context: Context, widgetId: String,): Int {
        val prefsKey = getKeyWithWidgetId(context, PREF_NUMBER_OF_DAYS, widgetId)
        return getPreferences(context).getInt(prefsKey, 7)
    }

    fun setNumberOfDays(context: Context, numberOfDays: Int, widgetId: String,) {
        val prefsKey = getKeyWithWidgetId(context, PREF_NUMBER_OF_DAYS, widgetId)
        getPreferences(context).edit().putInt(prefsKey, numberOfDays).apply()
    }

    fun getTextColor(context: Context, widgetId: String,): Color {
        val defaultColor = Color.White.toArgb()
        val prefsKey = getKeyWithWidgetId(context, PREF_TEXT_COLOR, widgetId)
        val colorInt = getPreferences(context).getInt(prefsKey, defaultColor)
        return Color(colorInt)
    }

    fun setTextColor(context: Context, textColor: Color, widgetId: String,) {
        val colorInt = textColor.toArgb()
        val prefsKey = getKeyWithWidgetId(context, PREF_TEXT_COLOR, widgetId)
        getPreferences(context).edit().putInt(prefsKey, colorInt).apply()
    }

    fun getFontSize(context: Context, widgetId: String,): FontSize {
        val prefsKey = getKeyWithWidgetId(context, PREF_FONT_SIZE, widgetId)
        val size = getPreferences(context).getString(prefsKey, FontSize.MEDIUM.name)
            ?: FontSize.MEDIUM.name
        return FontSize.valueOf(size)
    }

    fun setFontSize(context: Context, fontSize: FontSize, widgetId: String,) {
        val prefsKey = getKeyWithWidgetId(context, PREF_FONT_SIZE, widgetId)
        getPreferences(context).edit().putString(prefsKey, fontSize.name).apply()
    }

    fun getTextAlignment(context: Context, widgetId: String,): TextAlignment {
        val prefsKey = getKeyWithWidgetId(context, PREF_TEXT_ALIGNMENT, widgetId)
        val size = getPreferences(context).getString(prefsKey, TextAlignment.LEFT.name)
            ?: TextAlignment.LEFT.name
        return TextAlignment.valueOf(size)
    }

    fun setTextAlignment(context: Context, textAlignment: TextAlignment, widgetId: String,) {
        val prefsKey = getKeyWithWidgetId(context, PREF_TEXT_ALIGNMENT, widgetId)
        getPreferences(context).edit().putString(prefsKey, textAlignment.name).apply()
    }

    fun getOpacity(context: Context, widgetId: String,): Float {
        val prefsKey = getKeyWithWidgetId(context, PREF_OPACITY, widgetId)
        return getPreferences(context).getFloat(prefsKey, 0f)
    }

    fun setOpacity(context: Context, opacity: Float, widgetId: String,) {
        val prefsKey = getKeyWithWidgetId(context, PREF_OPACITY, widgetId)
        getPreferences(context).edit().putFloat(prefsKey, opacity).apply()
    }

    fun getHourFormat12(context: Context, widgetId: String,): Boolean {
        val prefsKey = getKeyWithWidgetId(context, PREF_HOUR_FORMAT_12, widgetId)
        return getPreferences(context).getBoolean(prefsKey, false)
    }

    fun setHourFormat12(context: Context, use12HourFormat: Boolean, widgetId: String,) {
        val prefsKey = getKeyWithWidgetId(context, PREF_HOUR_FORMAT_12, widgetId)
        getPreferences(context).edit().putBoolean(prefsKey, use12HourFormat).apply()
    }

    fun setSeparatorVisible(context: Context, visible: Boolean, widgetId: String,) {
        val prefsKey = getKeyWithWidgetId(context, PREF_SEPARATOR_VISIBLE, widgetId)
        getPreferences(context).edit().putBoolean(prefsKey, visible).apply()
    }

    fun getSeparatorVisible(context: Context, widgetId: String,): Boolean {
        val prefsKey = getKeyWithWidgetId(context, PREF_SEPARATOR_VISIBLE, widgetId)
        return getPreferences(context).getBoolean(prefsKey, false)
    }

    fun getLastReviewPrompt(context: Context): Long {
        return getPreferences(context).getLong(PREF_LAST_REVIEW_PROMPT, 0)
    }

    fun setLastReviewPrompt(context: Context, lastReviewPrompt: Long) {
        getPreferences(context).edit().putLong(PREF_LAST_REVIEW_PROMPT, lastReviewPrompt).apply()
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(
            context.packageName + "_preferences",
            Context.MODE_PRIVATE
        )
    }

}